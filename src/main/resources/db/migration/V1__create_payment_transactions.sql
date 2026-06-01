create table payment_transactions (
    id uuid primary key,
    ecommerce_order_id varchar(100) not null,
    gateway_payment_id varchar(100),
    amount_in_cents bigint not null,
    currency varchar(3) not null,
    status varchar(30) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_payment_transactions_ecommerce_order_id
    on payment_transactions (ecommerce_order_id);

create index idx_payment_transactions_gateway_payment_id
    on payment_transactions (gateway_payment_id);
