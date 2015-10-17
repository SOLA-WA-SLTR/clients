package org.fao.sola.admin.web.beans.db;

import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.sola.common.ClaimStatusConstants;
import org.sola.cs.services.ejbs.claim.businesslogic.ClaimEJBLocal;
//import org.sola.services.ejb.administrative.businesslogic.AdministrativeEJBLocal;
import org.sola.cs.services.ejb.search.businesslogic.SearchCSEJBLocal;
import org.sola.cs.services.ejb.search.repository.entities.ClaimSearchParams;
import org.sola.cs.services.ejb.search.repository.entities.ClaimSearchResult;

/**
 * Contains methods to migrate claims into SOLA Registry tables
 */
@Named
@ViewScoped
public class MigrationPageBean extends AbstractBackingBean {
    @EJB
    ClaimEJBLocal claimEjb;
    
    @EJB
    SearchCSEJBLocal searchEjb;
    
    //@EJB 
    //AdministrativeEJBLocal admEjb;
    
    private String log;
    
    public String getLog(){
        return log;
    }
    
    public void transfer(){
        // Get claims to transfer
        ClaimSearchParams params = new ClaimSearchParams();
        params.setStatusCode(ClaimStatusConstants.UNMODERATED);
        List<ClaimSearchResult> results = searchEjb.searchClaims(params);
        log = "SUCCESS = " + results.size();
    }
}
