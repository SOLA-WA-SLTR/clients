package org.fao.sola.admin.web.beans.db;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.apache.ibatis.exceptions.PersistenceException;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.sola.admin.opentenure.services.ejbs.claim.businesslogic.ClaimAdminEJBLocal;
import org.sola.common.ClaimStatusConstants;
import org.sola.common.StringUtility;
import org.sola.common.logging.LogUtility;
import org.sola.cs.services.ejb.search.businesslogic.SearchCSEJBLocal;
import org.sola.cs.services.ejb.search.repository.entities.ClaimSearchParams;
import org.sola.cs.services.ejb.search.repository.entities.ClaimSearchResult;
import org.sola.cs.services.ejbs.claim.businesslogic.ClaimEJBLocal;
import org.sola.cs.services.ejbs.claim.entities.Attachment;
import org.sola.cs.services.ejbs.claim.entities.AttachmentBinary;
import org.sola.cs.services.ejbs.claim.entities.Claim;
import org.sola.cs.services.ejbs.claim.entities.ClaimParty;
import org.sola.cs.services.ejbs.claim.entities.ClaimShare;
import org.sola.cs.services.ejbs.claim.entities.FieldPayload;
import org.sola.cs.services.ejbs.claim.entities.FieldType;
import org.sola.cs.services.ejbs.claim.entities.SectionElementPayload;
import org.sola.cs.services.ejbs.claim.entities.SectionPayload;
import org.sola.services.common.br.ValidationResult;
import org.sola.services.common.faults.FaultUtility;
import org.sola.services.common.faults.SOLAValidationException;
import org.sola.services.digitalarchive.businesslogic.DigitalArchiveEJBLocal;
import org.sola.services.digitalarchive.repository.entities.Document;
import org.sola.services.ejb.address.repository.entities.Address;
import org.sola.services.ejb.administrative.businesslogic.AdministrativeEJBLocal;
import org.sola.services.ejb.administrative.repository.entities.BaUnit;
import org.sola.services.ejb.administrative.repository.entities.BaUnitArea;
import org.sola.services.ejb.administrative.repository.entities.BaUnitDetail;
import org.sola.services.ejb.administrative.repository.entities.BaUnitNotation;
import org.sola.services.ejb.administrative.repository.entities.Rrr;
import org.sola.services.ejb.administrative.repository.entities.RrrShare;
import org.sola.services.ejb.cadastre.repository.entities.CadastreObject;
import org.sola.services.ejb.party.repository.entities.Party;
import org.sola.services.ejb.source.repository.entities.Source;
import org.sola.services.ejb.system.repository.entities.BrValidation;

/**
 * Contains methods to migrate claims into SOLA Registry tables
 */
@Named
@ViewScoped
public class MigrationPageBean extends AbstractBackingBean {

    @EJB
    AdministrativeEJBLocal admEjb;

    @EJB
    SearchCSEJBLocal searchEjb;

    @EJB
    ClaimAdminEJBLocal claimAdminEjb;

    @EJB
    ClaimEJBLocal claimEjb;

    @EJB
    DigitalArchiveEJBLocal archiveEjb;

    private String log;

    public String getLog() {
        return log;
    }

    public void transfer() {
        // Get claims to transfer
        ClaimSearchParams params = new ClaimSearchParams();
        params.setStatusCode(ClaimStatusConstants.UNMODERATED);
        List<ClaimSearchResult> results = searchEjb.searchClaims(params);
        int claimsTotal = results.size();
        int claimsLoaded = 0;
        int claimsFailed = 0;
        log = "";

        // Iterate through claims
        for (final ClaimSearchResult claimSearch : results) {
            try {
                runUpdate(new Runnable() {
                    @Override
                    public void run() {
                        // Get claim
                        Claim claim = claimEjb.getClaim(claimSearch.getId());

                        // Create BA unit and populate
                        BaUnit baUnit = new BaUnit();
                        baUnit.setBaUnitDetailList(new ArrayList<BaUnitDetail>());
                        baUnit.setName(claim.getDescription());

                        // Dynamic form
                        String nameFirstPart = "";
                        String nameLastPart = "";
                        BigDecimal baUnitSize = null;

                        if (claim.getDynamicForm() != null) {
                            FieldPayload f = getFieldPayload(claim, "volume");
                            if (f != null) {
                                nameFirstPart = getStringFieldValue(f);
                            }
                            f = getFieldPayload(claim, "folio");
                            if (f != null) {
                                nameLastPart = getStringFieldValue(f);
                            }
                            f = getFieldPayload(claim, "areaCofOhectares");
                            if (f != null && f.getBigDecimalPayload() != null) {
                                baUnitSize = f.getBigDecimalPayload().multiply(BigDecimal.valueOf(10000));
                            }

                            // Set cOfO
                            BaUnitDetail baDetails = new BaUnitDetail();
                            baDetails.setDetailCode("cOfO");
                            baDetails.setCustomDetailText(claim.getDescription());
                            baUnit.getBaUnitDetailList().add(baDetails);

                            // Set other details
                            addBaUnitDeatils(baUnit, claim, "advancePayment");
                            addBaUnitDeatils(baUnit, claim, "block");
                            addBaUnitDeatils(baUnit, claim, "dateSigned");
                            addBaUnitDeatils(baUnit, claim, "dateRegistered");
                            addBaUnitDeatils(baUnit, claim, "estate");
                            addBaUnitDeatils(baUnit, claim, "instrumentRegistrationNo");
                            addBaUnitDeatils(baUnit, claim, "LGA");
                            addBaUnitDeatils(baUnit, claim, "location");
                            addBaUnitDeatils(baUnit, claim, "layoutPlan");
                            addBaUnitDeatils(baUnit, claim, "plotNum");
                            addBaUnitDeatils(baUnit, claim, "cOfOtype");
                            addBaUnitDeatils(baUnit, claim, "yearlyRent");
                            addBaUnitDeatils(baUnit, claim, "reviewPeriod");
                            addBaUnitDeatils(baUnit, claim, "dateCommenced");
                            addBaUnitDeatils(baUnit, claim, "term");
                            addBaUnitDeatils(baUnit, claim, "zone");
                            addBaUnitDeatils(baUnit, claim, "IntellMapSheet");
                            addBaUnitDeatils(baUnit, claim, "valueTodevelope");
                            addBaUnitDeatils(baUnit, claim, "yearsTodevelope");
                        }

                        // Cadastre object
                        BaUnitArea baUnitArea = new BaUnitArea();
                        baUnitArea.setBaUnitId(baUnit.getId());
                        baUnitArea.setTypeCode("officialArea");
                        CadastreObject co = new CadastreObject();
                        if (!StringUtility.isEmpty(nameFirstPart)) {
                            baUnit.setNameFirstpart(nameFirstPart);
                            co.setNameFirstpart(nameFirstPart);
                        }
                        if (!StringUtility.isEmpty(nameLastPart)) {
                            baUnit.setNameLastpart(nameLastPart);
                            co.setNameLastpart(nameLastPart);
                        }
                        co.setLandUseCode(claim.getLandUseCode());

                        if (claim.getMappedGeometry() != null && claim.getMappedGeometry().length() > 0) {
                            // Add geometry
                            WKTReader wktReader = new WKTReader();
                            Geometry geom;
                            try {
                                geom = wktReader.read(claim.getMappedGeometry());
                                if (baUnitSize == null) {
                                    baUnitArea.setTypeCode("calculatedArea");
                                    baUnitArea.setSize(BigDecimal.valueOf(geom.getArea()));
                                }
                                geom.setSRID(32632);
                                WKBWriter wkbWriter = new WKBWriter();
                                co.setGeomPolygon(wkbWriter.write(geom));
                            } catch (ParseException ex) {
                                throw new RuntimeException(ex);
                            }
                        }

                        if (baUnitSize != null) {
                            baUnitArea.setSize(baUnitSize);
                        } else {
                            baUnitArea.setSize(BigDecimal.ZERO);
                        }

                        baUnit.setCadastreObjectList(new ArrayList<CadastreObject>());
                        baUnit.getCadastreObjectList().add(co);

                        // Set BaUnit name
                        if (StringUtility.isEmpty(baUnit.getName())) {
                            if (!StringUtility.isEmpty(baUnit.getNameFirstpart())
                                    || !StringUtility.isEmpty(baUnit.getNameLastpart())) {
                                baUnit.setName(StringUtility.empty(baUnit.getNameFirstpart()) + "\\"
                                        + StringUtility.empty(baUnit.getNameLastpart()));
                            }
                        }

                        if (StringUtility.isEmpty(baUnit.getName())) {
                            baUnit.setName("BaUnit");
                        }

                        // Rrr
                        Rrr rrr = new Rrr();
                        rrr.setBaUnitId(baUnit.getId());
                        rrr.setPrimary(true);
                        rrr.setRegistrationDate(Calendar.getInstance().getTime());
                        if (!StringUtility.isEmpty(claim.getTypeCode())) {
                            rrr.setTypeCode(claim.getTypeCode());
                        } else {
                            rrr.setTypeCode("ownership");
                        }

                        rrr.setRrrShareList(new ArrayList<RrrShare>());

                        BaUnitNotation notation = new BaUnitNotation();
                        notation.setRrrId(baUnit.getId());
                        notation.setNotationText(rrr.getTypeCode());
                        rrr.setNotation(notation);

                        if (claim.getShares() != null) {
                            for (ClaimShare claimShare : claim.getShares()) {
                                // Calculate denominator
                                short denominator = 1;
                                if (claimShare.getPercentage() != 0) {
                                    denominator = (short) (100 / claimShare.getPercentage());
                                }
                                RrrShare rrrShare = new RrrShare();
                                rrrShare.setRrrId(rrr.getId());
                                rrrShare.setNominator((short) 1);
                                rrrShare.setDenominator(denominator);
                                rrrShare.setRightHolderList(new ArrayList<Party>());
                                if (claimShare.getOwners() != null) {
                                    for (ClaimParty claimParty : claimShare.getOwners()) {
                                        Party party = new Party();
                                        if (!StringUtility.isEmpty(claimParty.getAddress())) {
                                            Address address = new Address();
                                            address.setDescription(claimParty.getAddress());
                                            party.setAddress(address);
                                        }
                                        party.setBirthDate(claimParty.getBirthDate());
                                        party.setEmail(claimParty.getEmail());
                                        party.setGenderCode(claimParty.getGenderCode());
                                        party.setIdNumber(claimParty.getIdNumber());
                                        party.setIdTypeCode(claimParty.getIdTypeCode());
                                        party.setLastName(claimParty.getLastName());
                                        party.setMobile(claimParty.getMobilePhone());
                                        party.setName(claimParty.getName());
                                        party.setPhone(claimParty.getPhone());
                                        if (claimParty.isPerson()) {
                                            party.setTypeCode("naturalPerson");
                                        } else {
                                            party.setTypeCode("nonNaturalPerson");
                                        }
                                        rrrShare.getRightHolderList().add(party);
                                    }
                                }
                                rrr.getRrrShareList().add(rrrShare);
                            }
                        }

                        // Documents
                        rrr.setSourceList(new ArrayList<Source>());
                        if (claim.getAttachments() != null) {
                            for (Attachment attachment : claim.getAttachments()) {
                                Source source = new Source();
                                AttachmentBinary attachBinary = claimEjb.getAttachment(attachment.getId());

                                if (attachBinary.getBody() != null && attachBinary.getBody().length > 0) {
                                    Document doc = new Document();
                                    doc.setBody(attachBinary.getBody());
                                    doc.setDescription(attachBinary.getDescription());
                                    doc.setExtension(attachBinary.getFileExtension());
                                    doc.setMimeType(attachBinary.getMimeType());
                                    archiveEjb.createDocument(doc);
                                    source.setArchiveDocumentId(doc.getId());
                                }

                                source.setDescription(attachment.getDescription());
                                source.setRecordation(attachment.getDocumentDate());
                                source.setReferenceNr(attachment.getReferenceNr());
                                source.setTypeCode(attachment.getTypeCode());

                                rrr.getSourceList().add(source);
                            }
                        }

                        // Add rrr to BaUnit
                        baUnit.setRrrList(new ArrayList<Rrr>());
                        baUnit.getRrrList().add(rrr);

                        // Import BaUnit
                        if (admEjb.importBaUnit(baUnit)) {
                            // Save BaUnit calculated area
                            admEjb.createBaUnitArea(baUnit.getId(), baUnitArea);

                            // Change claim status
                            claimAdminEjb.changeClaimStatus(claim.getId(), ClaimStatusConstants.MODERATED);
                        }
                    }
                });
                claimsLoaded += 1;

            } catch (Exception e) {
                LogUtility.log("Failed to load claim", e);
                claimsFailed += 1;
                log += String.format("Failed to load claim #%s with the follwoing error:\r\n", claimSearch.getNr());

                if (FaultUtility.hasCause(e, SOLAValidationException.class)) {
                    List<ValidationResult> validationResults = ((SOLAValidationException) FaultUtility
                            .getCause(e, SOLAValidationException.class))
                            .getValidationResultList();
                    for (ValidationResult result : validationResults) {
                        if (result.getSeverity().equals(BrValidation.SEVERITY_CRITICAL)) {
                            log += String.format("- %s\r\n", result.getFeedback());
                        }
                    }
                } else if (FaultUtility.hasCause(e, PersistenceException.class)) {
                    PersistenceException ex = FaultUtility.getCause(e, PersistenceException.class);
                    if (ex.getCause() != null) {
                        log += StringUtility.empty(ex.getCause().getMessage());
                    } else {
                        log += StringUtility.empty(ex.getMessage());
                    }
                } else {
                    log += LogUtility.getStackTraceAsString(e);
                }
                log += "\r\n";
            }
        }

        // Put summary on top of the log
        log = "===============================================\r\n" + log;
        if (claimsFailed > 0) {
            log = String.format("Failed %s claim(s)\r\n", claimsFailed) + log;
        }
        log = String.format("Loaded %s claim(s)\r\n", claimsLoaded) + log;
        log = String.format("Found %s claim(s)\r\n", claimsTotal) + log;
    }

    private void addBaUnitDeatils(BaUnit baUnit, Claim claim, String code) {
        FieldPayload f = getFieldPayload(claim, code);
        if (f != null) {
            BaUnitDetail baDetails = new BaUnitDetail();
            baDetails.setDetailCode(code);
            baDetails.setCustomDetailText(getStringFieldValue(f));
            baUnit.getBaUnitDetailList().add(baDetails);
        }
    }

    private String getStringFieldValue(FieldPayload f) {
        if (FieldType.TYPE_DECIMAL.equalsIgnoreCase(f.getFieldType())) {
            if (f.getBigDecimalPayload() != null) {
                return f.getBigDecimalPayload().toPlainString();
            } else {
                return null;
            }
        } else if (FieldType.TYPE_BOOL.equalsIgnoreCase(f.getFieldType())) {
            return Boolean.toString(f.isBooleanPayload());
        } else if (FieldType.TYPE_INTEGER.equalsIgnoreCase(f.getFieldType())) {
            if (f.getBigDecimalPayload() != null) {
                return f.getBigDecimalPayload().toBigInteger().toString();
            } else {
                return null;
            }
        } else {
            return f.getStringPayload();
        }
    }

    private FieldPayload getFieldPayload(Claim claim, String fieldName) {
        if (claim.getDynamicForm() == null || claim.getDynamicForm().getSectionPayloadList() == null
                || StringUtility.isEmpty(fieldName)) {
            return null;
        }

        for (SectionPayload section : claim.getDynamicForm().getSectionPayloadList()) {
            if (section.getSectionElementPayloadList() != null) {
                for (SectionElementPayload sectionElement : section.getSectionElementPayloadList()) {
                    if (sectionElement.getFieldPayloadList() != null) {
                        for (FieldPayload field : sectionElement.getFieldPayloadList()) {
                            if (field.getName().equalsIgnoreCase(fieldName)) {
                                return field;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getExceptionMessage(Throwable e) {
        if (e != null) {
            if (!StringUtility.isEmpty(e.getMessage())) {
                return e.getMessage();
            } else {
                return getExceptionMessage(e.getCause());
            }
        }
        return "Unknown exception";
    }
}
