package org.fao.sola.admin.web.beans.system;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.fao.sola.admin.web.beans.helpers.MessagesKeys;
import org.sola.common.ConfigConstants;
import org.sola.common.RolesConstants;
import org.sola.common.StringUtility;
import org.sola.admin.services.ejb.system.businesslogic.SystemAdminEJBLocal;
import org.sola.admin.services.ejb.system.repository.entities.DbInfo;

/**
 * Contains methods and properties related to system settings
 */
@Named
@SessionScoped
public class SystemBean extends AbstractBackingBean {
    
    private DbInfo dbInfo;
    
    @Inject
    MessageProvider msgProvider;
    
    @EJB
    SystemAdminEJBLocal systemEjb;

    public DbInfo getDbInfo() {
        return dbInfo;
    }

    public void setDbInfo(DbInfo dbInfo) {
        this.dbInfo = dbInfo;
    }
    
    public SystemBean(){
        super();
    }
    
    @PostConstruct
    public void init(){
        if(systemEjb.isInRole(RolesConstants.ADMIN_MANAGE_SETTINGS)){
            dbInfo = systemEjb.getDatabaseInfo();
            if(dbInfo != null){
                if(StringUtility.isEmpty(dbInfo.getProductName())){
                    dbInfo.setProductName(msgProvider.getMessage(MessagesKeys.GENERAL_LABEL_UNKNOWN));
                }
            }
        }
    }
    
    public boolean getIsSolaRegistry(){
        return dbInfo != null && dbInfo.getProductCode() != null && dbInfo.getProductCode().equalsIgnoreCase(ConfigConstants.SOLA_REGISTRY);
    }
    
    public boolean getIsSolaSystematicRegistration(){
        return dbInfo != null && dbInfo.getProductCode() != null && dbInfo.getProductCode().equalsIgnoreCase(ConfigConstants.SOLA_SYSTEMATIC_REGISTRATION);
    }
    
    public boolean getIsSolaStateLand() {
        return dbInfo != null && dbInfo.getProductCode() != null && dbInfo.getProductCode().equalsIgnoreCase(ConfigConstants.SOLA_STATE_LAND);
    }
    
    public boolean getIsSolaCommunityServer() {
        return dbInfo != null && dbInfo.getProductCode() != null && dbInfo.getProductCode().equalsIgnoreCase(ConfigConstants.SOLA_COMMUNITY_SERVER);
    }
    
    /** Removes all system settings from the session scope. */
    public void flush(){
        dbInfo = null;
    }
}
