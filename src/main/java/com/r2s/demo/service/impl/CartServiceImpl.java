package com.r2s.demo.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.r2s.demo.dto.CartDTO;
import com.r2s.demo.dto.CartLineItemDTO;
import com.r2s.demo.dto.VariantProductDTO;
import com.r2s.demo.entity.Cart;
import com.r2s.demo.entity.CartLineItem;
import com.r2s.demo.entity.VariantProduct;
import com.r2s.demo.exception.ProductNotFoundException;
import com.r2s.demo.repository.CartLineItemRepository;
import com.r2s.demo.repository.CartRepository;
import com.r2s.demo.repository.VariantProductRepository;
import com.r2s.demo.service.CartService;

@Service
public class CartServiceImpl implements CartService {

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private VariantProductRepository variantProductRepository;

	@Autowired
	private CartLineItemRepository cartLineItemRepository;

	@Autowired
	private ModelMapper modelMapper;

	@Transactional
	@Override
	public CartDTO addProductToCart(Long cartId, CartLineItemDTO cartLineItemDTO) {

		VariantProduct variantProduct = variantProductRepository.findById(cartLineItemDTO.getVariantProductId())
				.orElseThrow(() -> new ProductNotFoundException("product not found"));

		Cart cart = cartRepository.findById(cartId).orElse(null);

		CartLineItem cartLineItem = modelMapper.map(cartLineItemDTO, CartLineItem.class);

		CartLineItem cartLineItem2 = cartLineItemRepository
				.findByVariantProductId(cartLineItem.getVariantProduct().getId());

		if (cartLineItem2 != null && cartLineItem2.isDelete() == false) {
			cartLineItem2.setQuantity(cartLineItem2.getQuantity() + cartLineItem.getQuantity());
			cartLineItemRepository.save(cartLineItem2);
			cart.getCartLineItems().add(cartLineItem2);
		} else {
			cartLineItem.setVariantProduct(variantProduct);
			cartLineItem.setPrice(variantProduct.getPrice());
			cartLineItem.setCart(cart);
			cartLineItemRepository.save(cartLineItem);
			cart.getCartLineItems().add(cartLineItem);
		}

		// tinh toan tong gia tri don hang
		BigDecimal total = calculateTotalPrice(cart.getCartLineItems());

		cart.setTotal(total);
		cart.setNumberProduct(cartLineItemRepository.numberProduct(cartId));
		cartRepository.save(cart);

		return modelMapper.map(cart, CartDTO.class);

	}

	private BigDecimal calculateTotalPrice(Set<CartLineItem> cartLineItems) {
		BigDecimal totalPrice = BigDecimal.ZERO;

		for (CartLineItem cartItem : cartLineItems) {
			if (cartItem.isDelete() == false) {
				BigDecimal price = cartItem.getVariantProduct().getPrice();
				int quantity = cartItem.getQuantity();
				BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));

				totalPrice = totalPrice.add(lineTotal);
			}
		}

		return totalPrice;
	}

	@Override
	public CartDTO clearCart(Long id) {

		Cart cart = cartRepository.findById(id).orElse(null);
		Set<CartLineItem> list = cart.getCartLineItems();
		for (CartLineItem cartItem : list) {
			cartItem.setDelete(true);
		}
		return modelMapper.map(cart, CartDTO.class);
	}

}
