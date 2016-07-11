package org.fao.sola.admin.web.beans.refdata;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.ErrorKeys;
import org.fao.sola.admin.web.beans.helpers.MessageBean;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.fao.sola.admin.web.beans.language.LanguageBean;
import org.sola.admin.services.ejb.refdata.businesslogic.RefDataAdminEJBLocal;
import org.sola.admin.services.ejb.refdata.entities.RrrType;
import org.sola.admin.services.ejb.search.businesslogic.SearchAdminEJBLocal;
import org.sola.admin.services.ejb.search.repository.entities.LeaseConditionTemplateSearchResults;
import org.sola.common.StringUtility;
import org.sola.services.common.EntityAction;
import org.sola.services.ejb.administrative.businesslogic.AdministrativeEJBLocal;
import org.sola.services.ejb.administrative.repository.entities.LeaseConditionTemplate;

@Named
@ViewScoped
public class LeaseConditionsTemplatePageBean extends AbstractBackingBean {

    private LeaseConditionTemplate template;
    private RrrType[] rrrTypes;
    private List<LeaseConditionTemplateSearchResults> templates;

    @EJB
    private AdministrativeEJBLocal admEjb;

    @EJB
    private SearchAdminEJBLocal searchEjb;

    @EJB
    private RefDataAdminEJBLocal refEjb;

    @Inject
    private LanguageBean langBean;

    @Inject
    MessageProvider msgProvider;

    @Inject
    MessageBean msgBean;

    public LeaseConditionTemplate getTemplate() {
        return template;
    }

    public RrrType[] getRrrTypes() {
        return rrrTypes;
    }

    public List<LeaseConditionTemplateSearchResults> getTemplates() {
        return templates;
    }

    public LeaseConditionsTemplatePageBean() {
        super();
    }

    @PostConstruct
    private void init() {
        loadList();
        List<RrrType> rrrTypesList = refEjb.getCodeEntityList(RrrType.class, langBean.getLocale());

        if (rrrTypesList != null) {
            rrrTypesList = (ArrayList<RrrType>)((ArrayList)rrrTypesList).clone();
            RrrType dummy = new RrrType();
            dummy.setCode("");
            dummy.setDisplayValue(" ");
            rrrTypesList.add(0, dummy);
            rrrTypes = rrrTypesList.toArray(new RrrType[rrrTypesList.size()]);
        }
    }

    private void loadList() {
        templates = searchEjb.getLeaseConditionTemplates(langBean.getLocale(), null);
    }

    public void loadTemplate(String id) {
        if (StringUtility.isEmpty(id)) {
            template = new LeaseConditionTemplate();
            template.setId(UUID.randomUUID().toString());
        } else {
            template = admEjb.getLeaseConditionTemplate(id);
        }
    }

    public void deleteTemplate(String id) {
        LeaseConditionTemplate tmp = admEjb.getLeaseConditionTemplate(id);
        if (tmp != null) {
            template.setEntityAction(EntityAction.DELETE);
            admEjb.saveLeaseConditionTemplate(template);
            loadList();
        }
    }

    public void saveTemplate() throws Exception {
        if (template != null) {
            // Validate
            String errors = "";
            if (StringUtility.isEmpty(template.getTemplateName())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.ADMINISTRATIVE_LEASE_CONDITIONS_NAME_IS_NULL) + "\r\n";
            }
            if (StringUtility.isEmpty(template.getTemplateText())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.ADMINISTRATIVE_LEASE_CONDITIONS_TEXT_IS_NULL) + "\r\n";
            }

            if (!errors.equals("")) {
                throw new Exception(errors);
            }

            admEjb.saveLeaseConditionTemplate(template);
            loadList();
        }
    }
}
