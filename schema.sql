-- DDL
create table roles (
  role_id bigint primary key generated always as identity,
  role_name text
);

create table actors (
  actor_id bigint primary key generated always as identity,
  name text,
  email text unique,
  phone text,
  password text,
  role_id bigint references roles (role_id)
);

create table authorities (
  authority_id bigint primary key generated always as identity,
  authority_name text
);

create table actor_authorities (
  actor_id bigint references actors (actor_id),
  authority_id bigint references authorities (authority_id),
  primary key (actor_id, authority_id)
);

create table categories (
  category_id bigint primary key generated always as identity,
  category_name text
);

create table products (
  product_id bigint primary key generated always as identity,
  name text,
  description text,
  category_id bigint references categories (category_id),
  price numeric,
  quantity int,
  image_url text
);

create table sites (
  site_id bigint primary key generated always as identity,
  name text,
  location text,
  manager_id bigint references actors (actor_id)
);

create table site_actors (
  site_id bigint references sites (site_id),
  actor_id bigint references actors (actor_id),
  primary key (site_id, actor_id)
);

create table zones (
  zone_id bigint primary key generated always as identity,
  zone_name text,
  site_id bigint references sites (site_id)
);

create table sales (
  sale_id bigint primary key generated always as identity,
  client_name text,
  site_id bigint references sites (site_id),
  worker_id bigint references actors (actor_id),
  zone_id bigint references zones (zone_id),
  sale_timestamp timestamp,
  total_price numeric
);

create table sale_products (
  sale_id bigint references sales (sale_id),
  product_id bigint references products (product_id),
  quantity int,
  primary key (sale_id, product_id)
);

-- RLS

ALTER TABLE Actors ENABLE ROW LEVEL SECURITY;
ALTER TABLE Roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE Authorities ENABLE ROW LEVEL SECURITY;
ALTER TABLE Actor_Authorities ENABLE ROW LEVEL SECURITY;
ALTER TABLE Products ENABLE ROW LEVEL SECURITY;
ALTER TABLE Categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE Sites ENABLE ROW LEVEL SECURITY;
ALTER TABLE Site_Actors ENABLE ROW LEVEL SECURITY;
ALTER TABLE Zones ENABLE ROW LEVEL SECURITY;
ALTER TABLE Sales ENABLE ROW LEVEL SECURITY;
ALTER TABLE Sale_Products ENABLE ROW LEVEL SECURITY;

