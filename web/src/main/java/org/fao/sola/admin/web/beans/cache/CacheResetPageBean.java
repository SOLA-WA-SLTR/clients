package org.fao.sola.admin.web.beans.cache;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.ErrorKeys;
import org.fao.sola.admin.web.beans.helpers.MessageBean;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.fao.sola.admin.web.beans.helpers.MessagesKeys;
import org.sola.admin.services.ejbs.admin.businesslogic.AdministratorEJBLocal;

/**
 * Contains methods and reset cache of the administrated application
 */
@Named
@RequestScoped
public class CacheResetPageBean extends AbstractBackingBean {
    @EJB
    AdministratorEJBLocal adminEjb;
    
    @Inject
    MessageBean msg;

    @Inject
    MessageProvider msgProvider;
    
    public CacheResetPageBean(){
        super();
    }
    
    public void resetCache(){
        if(adminEjb.resetCache()){
            msg.setSuccessMessage(msgProvider.getMessage(MessagesKeys.RESET_CACHE_PAGE_SUCCESS));
        } else {
            msg.setErrorMessage(msgProvider.getErrorMessage(ErrorKeys.RESET_CACHE_PAGE_FAILED));
        }
    }
}