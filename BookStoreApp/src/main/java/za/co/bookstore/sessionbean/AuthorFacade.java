/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.bookstore.sessionbean;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import za.co.bookstore.entity.Author;
import za.co.bookstore.entity.Book;

/**
 *
 * @author tobah
 */
@Stateless
public class AuthorFacade extends AbstractFacade<Author> {

    @PersistenceContext(unitName = "za.co.bookstore_bookstore_war_1.0-SNAPSHOTPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public AuthorFacade() {
        super(Author.class);
    }
    
    public List<Author> findAuthorByBook(Book book) {
        String sql = "SELECT a FROM Author a WHERE a.bookId.id = :id";
        TypedQuery createQuery = getEntityManager().createQuery(sql, Author.class);
        createQuery.setParameter("id", book.getId());
        return createQuery.getResultList();
    }
    
    
    
}
