-- 1) users (JWT 로그인용)
create table if not exists users (
    id bigserial primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    role varchar(30) not null default 'USER',
    created_at timestamptz not null default now()
);

create index if not exists idx_users_created_at on users(created_at);


-- 2) products
create table if not exists products (
    id bigserial primary key,
    name varchar(120) not null,
    price bigint not null check (price >= 0),
    stock bigint not null check (stock >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_products_name on products(name);


-- 3) orders
create table if not exists orders (
    id bigserial primary key,
    user_id bigint null,
    status varchar(30) not null,
    total_amount bigint not null check (total_amount >= 0),
    created_at timestamptz not null default now(),
    constraint fk_orders_user_id
        foreign key (user_id) references users(id)
        on delete set null
);

create index if not exists idx_orders_user_id on orders(user_id);
create index if not exists idx_orders_created_at on orders(created_at);


-- 4) order_items
create table if not exists order_items (
    id bigserial primary key,
    order_id bigint not null,
    product_id bigint not null,
    quantity bigint not null check (quantity > 0),
    unit_price bigint not null check (unit_price >= 0),
    line_amount bigint not null check (line_amount >= 0),
    constraint fk_order_items_order_id
        foreign key (order_id) references orders(id)
        on delete cascade,
    constraint fk_order_items_product_id
        foreign key (product_id) references products(id)
        on delete restrict
);

create index if not exists idx_order_items_order_id on order_items(order_id);
create index if not exists idx_order_items_product_id on order_items(product_id);