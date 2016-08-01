package org.fao.sola.admin.web.beans.br;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.language.LanguageBean;
import org.sola.services.common.LocalInfo;
import org.sola.admin.services.ejb.search.businesslogic.SearchAdminEJBLocal;
import org.sola.admin.services.ejb.search.repository.entities.BrSearchParams;
import org.sola.admin.services.ejb.search.repository.entities.BrSearchResult;
import org.sola.admin.services.ejb.system.businesslogic.SystemAdminEJBLocal;
import org.sola.admin.services.ejb.system.repository.entities.Br;
import org.sola.admin.services.ejb.refdata.businesslogic.RefDataAdminEJBLocal;
import org.sola.admin.services.ejb.refdata.entities.BrTechnicalType;
import org.sola.admin.services.ejb.refdata.entities.BrValidationTargetType;

/**
 * Contains methods and properties to manage {@link Br}s
 */
@Named
@ViewScoped
public class BrListPageBean extends AbstractBackingBean {

    private List<BrSearchResult> searchResults;
    private BrTechnicalType[] technicalTypes;
    private BrValidationTargetType[] targetTypes;
    private BrSearchParams searchParams;

    @EJB
    private SearchAdminEJBLocal searchEjb;

    @EJB
    private SystemAdminEJBLocal systemEjb;

    @EJB
    RefDataAdminEJBLocal refEjb;

    @Inject
    private LanguageBean langBean;

    public List<BrSearchResult> getSearchResults() {
        return searchResults;
    }

    public BrSearchParams getSearchParams() {
        return searchParams;
    }

    public BrTechnicalType[] getTechnicalTypes() {
        return technicalTypes;
    }

    public BrValidationTargetType[] getTargetTypes() {
        return targetTypes;
    }

    @PostConstruct
    private void init() {
        searchParams = new BrSearchParams();
        // Load lists
        List<BrTechnicalType> techTypesList = refEjb.getCodeEntityList(BrTechnicalType.class, langBean.getLocale());
        List<BrValidationTargetType> targetTypesList = refEjb.getCodeEntityList(BrValidationTargetType.class, langBean.getLocale());

        if (techTypesList != null) {
            techTypesList = (ArrayList<BrTechnicalType>)((ArrayList)techTypesList).clone();
            BrTechnicalType dummy = new BrTechnicalType();
            dummy.setCode("");
            dummy.setDisplayValue(" ");
            techTypesList.add(0, dummy);
            technicalTypes = techTypesList.toArray(new BrTechnicalType[techTypesList.size()]);
        }

        if (targetTypesList != null) {
            targetTypesList = (ArrayList<BrValidationTargetType>)((ArrayList)targetTypesList).clone();
            BrValidationTargetType dummy = new BrValidationTargetType();
            dummy.setCode("");
            dummy.setDisplayValue(" ");
            targetTypesList.add(0, dummy);
            targetTypes = targetTypesList.toArray(new BrValidationTargetType[targetTypesList.size()]);
        }
    }

    public void search() {
        searchResults = searchEjb.searchBr(searchParams, langBean.getLocale());
    }

    public void delete(final String brId) throws Exception {
        runUpdate(new Runnable() {
            @Override
            public void run() {
                LocalInfo.setBaseUrl(getApplicationUrl());
                systemEjb.deleteBr(brId);
                search();
            }
        });
    }
}
