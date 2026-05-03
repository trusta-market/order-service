package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.exception.InvalidEnumException;
import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidMoneyException;
import com.trustamarket.orderservice.order.domain.exception.InvalidNameException;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderType;

import java.util.UUID;

// 주문 생성 — POST /api/v1/orders
// Buyer/Seller/Product snapshot 정보는 클라이언트가 직접 전달 (cross-domain 호출 최소화)
public interface CreateOrderUseCase {

    // 영속화된 Order 반환 — 컨트롤러가 Response DTO로 매핑 (id/status/totalAmount/createdAt 노출)
    Order createOrder(CreateOrderCommand command);

    record CreateOrderCommand(
            UUID buyerId, String buyerName,
            UUID sellerId, String sellerName,
            UUID productId, String productName, long productPrice,
            OrderType type,
            long shippingFee
    ) {
        public CreateOrderCommand {
            if (buyerId == null) throw new InvalidIdException("buyerId");
            if (buyerName == null || buyerName.isBlank()) throw new InvalidNameException("buyerName");
            if (sellerId == null) throw new InvalidIdException("sellerId");
            if (sellerName == null || sellerName.isBlank()) throw new InvalidNameException("sellerName");
            if (productId == null) throw new InvalidIdException("productId");
            if (productName == null || productName.isBlank()) throw new InvalidNameException("productName");
            if (productPrice < 0) throw new InvalidMoneyException(productPrice);
            if (type == null) throw new InvalidEnumException("orderType");
            if (shippingFee < 0) throw new InvalidMoneyException(shippingFee);
        }
    }
}
