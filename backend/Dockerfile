FROM php:8.2-apache

# Enable Apache Mod Rewrite for clean URL routing
RUN a2enmod rewrite

# Install SQLite extensions (PDO SQLite)
RUN apt-get update && apt-get install -y \
    libsqlite3-dev \
    && docker-php-ext-install pdo_sqlite

# Set working directory
WORKDIR /var/www/html

# Copy backend files
COPY . /var/www/html/

# Set ownership to Apache www-data user for SQLite write permissions
RUN chown -R www-data:www-data /var/www/html

# Expose port 80
EXPOSE 80
