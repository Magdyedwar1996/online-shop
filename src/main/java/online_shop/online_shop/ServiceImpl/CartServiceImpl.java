package online_shop.online_shop.ServiceImpl;

import jakarta.transaction.Transactional;
import online_shop.online_shop.adapter.*;
import online_shop.online_shop.domain.*;
import online_shop.online_shop.dto.CartDto;
import online_shop.online_shop.dto.CartItemDto;
import online_shop.online_shop.dto.ProductDto;
import online_shop.online_shop.repository.*;
import online_shop.online_shop.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartServiceImpl implements CartService {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public CartDto getCartById(Long id) {
        Optional<Cart> cart = cartRepository.findById(id);
        return CartAdapter.getCartDtoFromCart(cart.get());
    }

    @Override
    public void addItemToCart(Long cartId, CartItemDto cartItemDto) {
        Optional<Cart> cartOptional = cartRepository.findById(cartId);
        if (cartOptional.isPresent()) {
            Cart cart = cartOptional.get();
            CartItem cartItem = CartItemAdapter.getCartItemFromCartItemDto(cartItemDto);
            Product product = ProductAdapter.getProductFromProductDto(cartItemDto.getProductDto());
            product.setPrice(cartItemDto.getProductDto().getPrice());

            // Ensure the category exists or is saved if new
            Category category = product.getCategory();
            if (category.getId() == null || !categoryRepository.existsById(category.getId())) {
                category = categoryRepository.save(category);
            }

            // Ensure the product exists or is saved if new
            if (product.getId() == null || !productRepository.existsById(product.getId())) {
                product.setCategory(category);
                product = productRepository.save(product);
            }

            cartItem.setProduct(product);
            cartItem.setCart(cart);
            cartItem.setPrice(product.getPrice());
            cartItemRepository.save(cartItem);

            cart.getItems().add(cartItem);
            double newTotalPrice = cart.getTotalPrice() + cartItem.getPrice() * cartItem.getQuantity();
            cart.setTotalPrice(newTotalPrice);
            cartRepository.save(cart);
        } else {
            throw new RuntimeException("Cart not found");
        }
    }

    @Override
    public void removeItemFromCart(Long cartId, Long itemId) {
        Optional<Cart> cartOptional = cartRepository.findById(cartId);
        if (cartOptional.isPresent()) {
            Cart cart = cartOptional.get();
            Optional<CartItem> cartItemOptional = cartItemRepository.findById(itemId);
            if (cartItemOptional.isPresent()) {
                CartItem cartItem = cartItemOptional.get();
                cart.getItems().remove(cartItem);
                double newTotalPrice = cart.getTotalPrice() - cartItem.getPrice() * cartItem.getQuantity();
                cart.setTotalPrice(newTotalPrice);
                cartItemRepository.delete(cartItem);
                cartRepository.save(cart);
            } else {
                throw new RuntimeException("CartItem not found");
            }
        } else {
            throw new RuntimeException("Cart not found");
        }
    }

    @Override
    public void clearCart(Long cartId) {
        Optional<Cart> cartOptional = cartRepository.findById(cartId);
        if (cartOptional.isPresent()) {
            cartRepository.delete(cartOptional.get());
        } else {
            throw new RuntimeException("Cart not found");
        }
    }

    @Override
    public List<CartDto> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        return carts.stream()
                .map(CartAdapter::getCartDtoFromCart)
                .collect(Collectors.toList());
    }
}