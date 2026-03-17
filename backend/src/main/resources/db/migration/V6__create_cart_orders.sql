CREATE TABLE carts (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT UNIQUE NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE cart_items (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cart_id      BIGINT NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    listing_id   BIGINT NOT NULL REFERENCES listings (id),
    price_at_add DECIMAL(10, 2) NOT NULL,
    added_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (cart_id, listing_id)
);

CREATE TABLE orders (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users (id),
    order_number      VARCHAR(20) UNIQUE NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    subtotal          DECIMAL(10, 2) NOT NULL,
    service_fee       DECIMAL(10, 2) NOT NULL,
    total             DECIMAL(10, 2) NOT NULL,
    payment_intent_id VARCHAR(255),
    payment_method    VARCHAR(30) NOT NULL,
    confirmed_at      TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_number ON orders (order_number);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    listing_id BIGINT NOT NULL REFERENCES listings (id),
    ticket_id  BIGINT NOT NULL REFERENCES tickets (id),
    price_paid DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
