ALTER TABLE categories DROP CONSTRAINT uq_categories_user_name_type;

CREATE UNIQUE INDEX uq_categories_user_type_lower_name
    ON categories (user_id, type, LOWER(name));
