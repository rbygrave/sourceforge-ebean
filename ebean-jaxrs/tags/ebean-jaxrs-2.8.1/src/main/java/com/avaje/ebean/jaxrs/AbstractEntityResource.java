package com.avaje.ebean.jaxrs;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.OrderBy;
import com.avaje.ebean.Query;

/**
 * Base class providing basic insert, update, delete, delete many, find by id
 * and find all functionality for entity beans of a defined type.
 * <p>
 * You can extend this to override or extend this to customise the queries used
 * (which properties to fetch) and JSON output produced (using JsonWriteOptions
 * etc).
 * </p>
 * 
 * @author rbygrave
 * 
 * @param <T>
 *            the entity bean type
 */
@Consumes( { MediaType.APPLICATION_JSON, "text/json" })
@Produces( { MediaType.APPLICATION_JSON, "text/json" })
public abstract class AbstractEntityResource<T> {

    private static final Logger logger = Logger.getLogger(AbstractEntityResource.class.getName());

    /**
     * The root entity bean type.
     */
    protected final Class<T> beanType;

    /**
     * The EbeanServer used.
     */
    protected final EbeanServer server;

    protected String defaultFindAllOrderBy;
    
    /**
     * Construct using the default EbeanServer and JsonContext.
     * 
     * @param beanType
     *            the entity bean type
     */
    protected AbstractEntityResource(Class<T> beanType) {
        this(beanType, Ebean.getServer(null));
    }

    /**
     * Construct with an explicit EbeanServer.
     * 
     * @param beanType
     *            the entity bean type
     */
    protected AbstractEntityResource(Class<T> beanType, EbeanServer server) {
        this.beanType = beanType;
        this.server = server;
    }

    /**
     * Insert a bean.
     * 
     * @param bean
     *            the bean to insert
     */
    @POST
    public Response insert(@Context UriInfo uriInfo, T bean) {

        server.save(bean);
        Object id = server.getBeanId(bean);

        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI createdUri = ub.path("" + id).build();

        return Response.created(createdUri).build();
    }

    /**
     * Update a bean.
     * 
     * @param id
     *            the unique id of the bean
     * @param bean
     *            the bean to update
     */
    @PUT
    @Path("/{id}")
    public void update(@PathParam("id") String id, T bean) {

        server.update(bean);
    }

    /**
     * Delete multiple beans using Id's from the "UriOptions".
     * 
     * @param options
     *            The UriOptions in the form "::(id1,id2,...)"
     */
    @DELETE
    public void deleteMultiple(@PathParam("uriOptions") String options) {

        UriOptions uriOptions = UriOptions.parse(options);
        List<String> idList = uriOptions.getIdList();
        if (idList == null || idList.isEmpty()) {
            throw new IllegalArgumentException("No List of Ids where set?");
        }

        // Ebean will do type conversion of the Id's for us
        server.delete(beanType, idList);
    }

    /**
     * Delete a bean using its id.
     * 
     * @param id
     *            the unique id of the bean.
     */
    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") String id) {

        // Ebean will do type conversion of the Id for us
        server.delete(beanType, id);
    }

    /**
     * Find a bean given its Id.
     * 
     * @param id
     *            the id of the bean.
     */
    @GET
    @Path("/{id}")
    public T find(@PathParam("id") String id, @PathParam("uriOptions") String uriOptions) {

        Query<T> query = server.find(beanType);

        if (!applyUriOptions(uriOptions, query)) {
            configDefaultFindByIdQuery(query);
        }

        // Ebean will do type conversion of the Id for us
        return query.setId(id).findUnique();
    }

    /**
     * Configure the "Find By Id" query.
     * <p>
     * This is only used when no PathProperties where set via UriOptions.
     * </p>
     * <p>
     * This effectively controls the "default" query used to render this bean.
     * </p>
     */
    protected void configDefaultFindByIdQuery(Query<T> query) {

    }

    /**
     * Find all the beans for this beanType.
     * <p>
     * This can use URL query parameters such as order and maxrows to configure
     * the query.
     * </p>
     */
    @GET
    public List<T> findAll(@Context UriInfo ui, @PathParam("uriOptions") String uriOptions) {

        Query<T> query = server.find(beanType);

        if (!applyUriOptions(uriOptions, query)) {
            configDefaultFindAllQuery(query);
        }

        configQuery(query, ui);

        if (defaultFindAllOrderBy != null){
            // see if we should use the default orderBy clause
            OrderBy<T> orderBy = query.orderBy();
            if (orderBy.isEmpty()){
                query.orderBy(defaultFindAllOrderBy);
            }
        }
        
        return query.findList();
    }

    /**
     * Configure the "Find All" query.
     * <p>
     * This is only used when no PathProperties where set via UriOptions.
     * </p>
     * <p>
     * This effectively controls the "default" query used with the find all
     * query.
     * </p>
     */
    protected void configDefaultFindAllQuery(Query<T> query) {

    }

    /**
     * Return true if PathProperties has been applied.
     */
    protected boolean applyUriOptions(String uriOptions, Query<?> query) {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("uriOptions: [" + uriOptions + "]");
        }

        UriOptions options = UriOptions.parse(uriOptions);
        return applyUriOptions(options, query);
    }

    /**
     * Apply PathProperties, sort etc from UriOptions to the query.
     * <p>
     * This returns true if the PathProperties where set. In this case the
     * default query configuration should not be used.
     * </p>
     */
    protected boolean applyUriOptions(UriOptions options, Query<?> query) {

        if (!options.isEmpty()) {
            options.apply(query);
            MarshalOptions.setPathProperties(options.getPathProperties());
            return options.hasPathProperties();
        }
        return false;
    }

    /**
     * Configure the query using some known URL query parameters.
     */
    protected void configQuery(Query<T> query, UriInfo ui) {

        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();

        String whereClause = getSingleParam(queryParams.get("where"));
        if (whereClause != null) {
            query.where(whereClause);
        }

        String orderByClause = getSingleParam(queryParams.get("sort"));
        if (orderByClause != null) {
            query.order(orderByClause);
        }

        Integer maxRows = getSingleIntegerParam(queryParams.get("maxrows"));
        if (maxRows != null) {
            query.setMaxRows(maxRows);
        }

        Integer firstRow = getSingleIntegerParam(queryParams.get("firstrow"));
        if (firstRow != null) {
            query.setFirstRow(firstRow);
        }
    }

    /**
     * Return a single Integer parameter.
     */
    protected Integer getSingleIntegerParam(List<String> list) {
        String s = getSingleParam(list);
        if (s != null) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Return a single parameter value.
     */
    protected String getSingleParam(List<String> list) {
        if (list != null && list.size() == 1) {
            return list.get(0);
        }
        return null;
    }
}
