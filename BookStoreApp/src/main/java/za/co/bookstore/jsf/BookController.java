package za.co.bookstore.jsf;

import java.io.File;
import java.io.IOException;
import za.co.bookstore.entity.Book;
import za.co.bookstore.jsf.util.JsfUtil;
import za.co.bookstore.jsf.util.PaginationHelper;
import za.co.bookstore.sessionbean.BookFacade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.ManagedBean;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import za.co.bookstore.entity.Author;
import za.co.bookstore.sessionbean.AuthorFacade;

/**
 *
 * @author tobah
 */
@Named("bookController")
@SessionScoped
public class BookController implements Serializable {

    private Book current;
    private Book selectedBook;
    private DataModel items = null;
    @EJB
    private za.co.bookstore.sessionbean.BookFacade ejbFacade;
    @EJB
    private za.co.bookstore.sessionbean.AuthorFacade ejbAuthorFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;

    public BookController() {
    }

    public Book getSelected() {
        if (current == null) {
            current = new Book();
            selectedItemIndex = -1;
        }
        return current;
    }

    private BookFacade getFacade() {
        return ejbFacade;
    }

    public AuthorFacade getEjbAuthorFacade() {
        return ejbAuthorFacade;
    }

    public Book getSelectedBook() {
        return selectedBook;
    }

    public void setSelectedBook(Book selectedBook) {
        this.selectedBook = selectedBook;
    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(10) {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public DataModel createPageDataModel() {
                    return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}));
                }
            };
        }
        return pagination;
    }

    public String prepareList() {
        recreateModel();
        return "/crud/book/List";
    }

    public String prepareView() {
        current = selectedBook;
        return "/crud/book/View";
    }

    public String prepareCreate() {
        current = new Book();
        selectedItemIndex = -1;
        return "/crud/book/Create";
    }

    //This method loads data from an xml file and displays it
    public String loadData() {
        current = new Book();
        try {
            
            //Getting the application context instance
            ServletContext ctx = (ServletContext) FacesContext
                    .getCurrentInstance()
                    .getExternalContext().getContext();
            
            //Using the application context to get the xml file path
            String path = ctx.getRealPath("/WEB-INF/xmlresources/bookstore.xml");
            
            File xmlFile
                    = new File(path);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);

            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("book");

            for (int i = 0; i < nList.getLength(); i++) {

                Element element = (Element) nList.item(i);

                //setting the book details of the current book object
                current.setCategory(element.getAttribute("category"));
                current.setTitle(element.getElementsByTagName("title")
                        .item(0).getTextContent());

                Double price = Double.parseDouble(element.getElementsByTagName("price")
                        .item(0).getTextContent());

                current.setPrice(price);
                current.setYear(element.getElementsByTagName("year")
                        .item(0).getTextContent());
                current.setTitle(element.getElementsByTagName("title")
                        .item(0).getTextContent());

                NodeList optionList = element.getElementsByTagName("author");

                List<Author> authList = new ArrayList<>();

                for (int j = 0; j < optionList.getLength(); ++j) {
                    Element authorList = (Element) optionList.item(j);

                    Author author = new Author();
                    
                    //setting the author details of the the current book object
                    author.setBookId(current);
                    author.setName(authorList.getFirstChild().getNodeValue());
                    authList.add(author);

                }
                current.setAuthorList(authList);
                
                //persisting the current book object into my database
                getFacade().create(current);
            }

        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(BookFacade.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "/crud/book/List";
    }

    public String create() {
        try {
            getFacade().create(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("BookCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String prepareEdit() {
        current = (Book) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("BookUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (Book) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreatePagination();
        recreateModel();
        return "List";
    }

    public String destroyAndView() {
        performDestroy();
        recreateModel();
        updateCurrentItem();
        if (selectedItemIndex >= 0) {
            return "View";
        } else {
            // all items were removed - go back to list
            recreateModel();
            return "List";
        }
    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("BookDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }

    private void updateCurrentItem() {
        int count = getFacade().count();
        if (selectedItemIndex >= count) {
            // selected index cannot be bigger than number of items:
            selectedItemIndex = count - 1;
            // go to previous page if last page disappeared:
            if (pagination.getPageFirstItem() >= count) {
                pagination.previousPage();
            }
        }
        if (selectedItemIndex >= 0) {
            current = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    public DataModel getItems() {
        items = new ListDataModel(ejbFacade.findAll());
        return items;
    }

    private void recreateModel() {
        items = null;
    }

    private void recreatePagination() {
        pagination = null;
    }

    public String next() {
        getPagination().nextPage();
        recreateModel();
        return "List";
    }

    public String previous() {
        getPagination().previousPage();
        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    public Book getBook(java.lang.Integer id) {
        return ejbFacade.find(id);
    }

    @FacesConverter(forClass = Book.class)
    public static class BookControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            BookController controller = (BookController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "bookController");
            return controller.getBook(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            java.lang.Integer key;
            key = Integer.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Integer value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Book) {
                Book o = (Book) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + Book.class.getName());
            }
        }

    }

}
