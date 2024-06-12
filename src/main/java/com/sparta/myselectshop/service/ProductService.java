package com.sparta.myselectshop.service;

import com.sparta.myselectshop.dto.ProductMypriceRequestDto;
import com.sparta.myselectshop.dto.ProductRequestDto;
import com.sparta.myselectshop.dto.ProductResponseDto;
import com.sparta.myselectshop.entity.*;
import com.sparta.myselectshop.naver.dto.ItemDto;
import com.sparta.myselectshop.repository.FolderRepository;
import com.sparta.myselectshop.repository.ProductFolderRepository;
import com.sparta.myselectshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final FolderRepository folderRepository;
    private final ProductFolderRepository productFolderRepository;
    public static final int MIN_MY_PRICE = 100;


    public ProductResponseDto createProduct(ProductRequestDto requestDto, User user) {
        //받아온 dto를 객체로 만들어줌
        Product product = productRepository.save(new Product(requestDto, user));
        return new ProductResponseDto(product);
    }

    @Transactional
    //변경감지
    public ProductResponseDto updateProduct(Long id, ProductMypriceRequestDto requestDto) {
        int myprice = requestDto.getMyprice();
        if(myprice < MIN_MY_PRICE) {
            throw new IllegalArgumentException("유효하지 않은 관심 가격입니다. 최소 " + MIN_MY_PRICE + "원 이상으로 설정해주세요.");
        }

        Product product = productRepository.findById(id).orElseThrow(() ->
                new NullPointerException("해당 상품을 찾을 수 없습니다.")
                );

        product.update(requestDto);

        return new ProductResponseDto(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProducts(User user, int page, int size, String sortBy, boolean isAsc) {
        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        UserRoleEnum userRoleEnum = user.getRole();

        Page<Product> productList;

        if(userRoleEnum == UserRoleEnum.USER) {
            productList = productRepository.findAllByUser(user,pageable);
        } else {
            productList = productRepository.findAll(pageable);
        }
        return productList.map(ProductResponseDto::new);
    }

    @Transactional
    public void updateBySearch(Long id, ItemDto itemDto) {
        Product product = productRepository.findById(id).orElseThrow(()->
                new NullPointerException("해당 상품은 존재하지 않습니다.")
                );
        product.updateByItemDto(itemDto);
    }


    public void addFolder(Long productId, Long folderId, User user) {
        //상품 조회
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new NullPointerException("해당 상품이 존재하지 않습니다.")
        );

        //폴더 조회
        Folder folder = folderRepository.findById(folderId).orElseThrow(
                () -> new NullPointerException("해당 폴더가 존재하지 않습니다.")
        );

        //해당 유저가 등록한 상품과 폴더가 맞는지 확인
        if (!product.getUser().getId().equals(user.getId())
        || !folder.getUser().getId().equals(user.getId())
        ) {
            throw new IllegalArgumentException("회원님의 관심상품이 아니거나, 회원님의 폴더가 아닙니다.");
        }

        //상품에 폴더를 추가
        //해당 폴더가 이미 등록된 폴더인지 확인해야 함. -> 중복확인 폴더

        //아래 쿼리를 통해 row가 존재한다면 이미 폴더에 상품이 있다고 볼 수 있음
        Optional<ProductFolder> overlapFolder = productFolderRepository.findByProductAndFolder(product, folder);

        if(overlapFolder.isPresent()) {
            throw new IllegalArgumentException("중복된 폴더입니다.");
        }

        //repository의 한 줄은 객체이므로 객체로 저장한다.
        //save vs saveAll
        //단건 저장 vs 여러건 저장(리스트)
        productFolderRepository.save(new ProductFolder(product, folder));

    }
}
