package dev.shelfie.filters;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

interface BookRepository extends Repository<Book, Long>, JpaSpecificationExecutor<Book> {}
