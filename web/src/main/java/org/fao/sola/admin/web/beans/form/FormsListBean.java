package org.fao.sola.admin.web.beans.form;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Part;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.ErrorKeys;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.fao.sola.admin.web.beans.language.LanguageBean;
import org.sola.admin.opentenure.services.ejbs.claim.businesslogic.ClaimAdminEJBLocal;
import org.sola.admin.opentenure.services.ejbs.claim.entities.FormTemplate;
import org.sola.common.StringUtility;
import org.sola.services.common.EntityAction;
import org.sola.services.common.logging.LogUtility;

/**
 * Contains methods to get list of available dynamic forms
 */
@Named
@ViewScoped
public class FormsListBean extends AbstractBackingBean {

    @EJB
    ClaimAdminEJBLocal claimEjb;

    @Inject
    LanguageBean langBean;

    @Inject
    MessageProvider msgProvider;

    private FormTemplate[] forms;

    private Part formFile;

    private FormTemplate formTemplate;
    
    public FormsListBean() {
    }

    @PostConstruct
    private void init() {
        List<FormTemplate> formsList = claimEjb.getFormTemplates(null);
        formsList = (ArrayList<FormTemplate>)((ArrayList)formsList).clone();
        if (formsList != null) {
            forms = formsList.toArray(new FormTemplate[formsList.size()]);
        }
    }

    public Part getFormFile() {
        return formFile;
    }

    public void setFormFile(Part formFile) {
        this.formFile = formFile;
    }

    public FormTemplate[] getForms() {
        return forms;
    }

    public boolean canBeDeleted(String formName) {
        return claimEjb.checkFormTemplateHasPayload(formName);
    }

    public void export(FormTemplate fTmpl) {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        ec.responseReset();
        ec.setResponseContentType("application/octet-stream");
        //ec.setResponseContentLength(fTmpl);
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"form.ser\"");

        try {
            OutputStream output = ec.getResponseOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(output);
            out.writeObject(fTmpl);
            out.close();
        } catch (Exception e) {
            getContext().addMessage(null, new FacesMessage(e.getMessage()));
        }
        fc.responseComplete();
    }

    public void importForm() throws Exception {
        // Make checks
        String errors = "";

        if (formFile == null) {
            errors += "- " + msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_SELECT_FORM_FILE) + "\r\n";
        }

        if (!StringUtility.isEmpty(errors)) {
            throw new Exception(errors);
        }

        try {
            ObjectInputStream in = new ObjectInputStream(formFile.getInputStream());
            formTemplate = (FormTemplate) in.readObject();
            in.close();
            runUpdate(new Runnable() {
                @Override
                public void run() {
                    claimEjb.saveFormTemplate(formTemplate);
                }
            });
            init();
        } catch (Exception e) {
            LogUtility.log("Failed to import form", e);
            throw e;
        }
    }

    public void makeDefault(FormTemplate fTmpl) {
        try {
            if (fTmpl == null) {
                return;
            }
            claimEjb.makeFormDefault(fTmpl.getName());
            init();
        } catch (Exception e) {
            getContext().addMessage(null, new FacesMessage(processException(e, langBean.getLocale()).getMessage()));
        }
    }

    public void delete(FormTemplate fTmpl) {
        try {
            if (fTmpl == null) {
                return;
            }

            // Check for existing payload records
            if (claimEjb.checkFormTemplateHasPayload(fTmpl.getName())) {
                getContext().addMessage(null, new FacesMessage(msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FORM_HAS_PAYLOAD)));
                return;
            }

            fTmpl.setEntityAction(EntityAction.DELETE);
            claimEjb.saveFormTemplate(fTmpl);
            init();
        } catch (Exception e) {
            getContext().addMessage(null, new FacesMessage(processException(e, langBean.getLocale()).getMessage()));
        }
    }
}
