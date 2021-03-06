/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.config;

import static java.util.Arrays.asList;

import org.openconcerto.erp.core.common.component.SocieteCommonSQLElement;
import org.openconcerto.erp.core.common.element.AdresseCommonSQLElement;
import org.openconcerto.erp.core.common.element.AdresseSQLElement;
import org.openconcerto.erp.core.common.element.BanqueSQLElement;
import org.openconcerto.erp.core.common.element.DepartementSQLElement;
import org.openconcerto.erp.core.common.element.LangueSQLElement;
import org.openconcerto.erp.core.common.element.MoisSQLElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.element.PaysSQLElement;
import org.openconcerto.erp.core.common.element.StyleSQLElement;
import org.openconcerto.erp.core.common.element.TitrePersonnelSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.ClientDepartementSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.CompteClientTransactionSQLELement;
import org.openconcerto.erp.core.customerrelationship.customer.element.ContactSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.ContactSQLElement.ContactAdministratifSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.ContactSQLElement.ContactFournisseurSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.CourrierClientSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.CustomerCategorySQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.CustomerSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.ModeleCourrierClientSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.ReferenceClientSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.RelanceSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.TypeLettreRelanceSQLElement;
import org.openconcerto.erp.core.edm.AttachmentSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.AssociationAnalytiqueSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.AssociationCompteAnalytiqueSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.AxeAnalytiqueSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCGSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.DeviseHistoriqueSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.ExerciceCommonSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.NatureCompteSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.PieceSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.PosteAnalytiqueSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.RepartitionAnalytiqueSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.SaisieKmItemSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.SaisieKmSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.TypeComptePCGSQLElement;
import org.openconcerto.erp.core.finance.accounting.model.Currency;
import org.openconcerto.erp.core.finance.payment.element.ChequeAEncaisserSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ChequeAvoirClientSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ChequeFournisseurSQLElement;
import org.openconcerto.erp.core.finance.payment.element.EncaisserMontantElementSQLElement;
import org.openconcerto.erp.core.finance.payment.element.EncaisserMontantSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ReglerMontantElementSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ReglerMontantSQLElement;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.core.finance.tax.element.EcoTaxeSQLElement;
import org.openconcerto.erp.core.finance.tax.element.TaxeComplementaireSQLElement;
import org.openconcerto.erp.core.finance.tax.element.TaxeSQLElement;
import org.openconcerto.erp.core.humanresources.employe.SituationFamilialeSQLElement;
import org.openconcerto.erp.core.humanresources.employe.element.CommercialSQLElement;
import org.openconcerto.erp.core.humanresources.employe.element.EtatCivilSQLElement;
import org.openconcerto.erp.core.humanresources.employe.element.ObjectifSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.AcompteSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.AyantDroitContratPrevSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.AyantDroitSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.AyantDroitTypeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CaisseCotisationRenseignentSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CaisseCotisationSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CaisseModePaiementSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ClassementConventionnelSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeBaseAssujettieSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeCaisseTypeRubriqueSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeCaractActiviteSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeContratTravailSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeCotisationEtablissementSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeCotisationIndividuelleSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeDroitContratSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeEmploiSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeIdccSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodePenibiliteContratSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodePenibiliteSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeRegimeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeStatutCategorielConventionnelSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeStatutCategorielSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeStatutProfSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeTypeRubriqueBrutSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CoefficientPrimeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratDetacheExpatrieSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratDispositifPolitiqueSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratModaliteTempsSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratMotifRecoursSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratPrevoyanceRubriqueNetSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratPrevoyanceRubriqueSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratPrevoyanceSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratPrevoyanceSalarieSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratRegimeMaladieSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratRegimeVieillesseSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratSalarieSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CumulsCongesSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CumulsPayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.DSNNatureSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.FichePayeElementSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.FichePayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ImpressionRubriqueSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.InfosSalariePayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ModeReglementPayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.MotifArretTravailSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.MotifFinContratSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.MotifRepriseArretTravailSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.PeriodeValiditeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ProfilPayeElementSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ProfilPayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.RegimeBaseSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ReglementPayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.RubriqueBrutSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.RubriqueCommSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.RubriqueCotisationSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.RubriqueNetSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.SalarieSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.TypeComposantBaseAssujettieSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.TypePreavisSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.TypeRubriqueBrutSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.TypeRubriqueNetSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.VariablePayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.VariableSalarieSQLElement;
import org.openconcerto.erp.core.humanresources.timesheet.element.PointageSQLElement;
import org.openconcerto.erp.core.project.element.CalendarItemGroupSQLElement;
import org.openconcerto.erp.core.project.element.CalendarItemSQLElement;
import org.openconcerto.erp.core.sales.credit.element.AvoirClientElementSQLElement;
import org.openconcerto.erp.core.sales.credit.element.AvoirClientSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.EcheanceClientSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureItemSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.TransferInvoiceSQLElement;
import org.openconcerto.erp.core.sales.order.element.CommandeClientElementSQLElement;
import org.openconcerto.erp.core.sales.order.element.CommandeClientSQLElement;
import org.openconcerto.erp.core.sales.order.element.TransferCustomerOrderSQLElement;
import org.openconcerto.erp.core.sales.pos.element.CaisseTicketSQLElement;
import org.openconcerto.erp.core.sales.pos.element.SaisieVenteComptoirSQLElement;
import org.openconcerto.erp.core.sales.pos.element.TicketCaisseSQLElement;
import org.openconcerto.erp.core.sales.pos.io.BarcodeReader;
import org.openconcerto.erp.core.sales.price.element.DeviseSQLElement;
import org.openconcerto.erp.core.sales.price.element.TarifSQLElement;
import org.openconcerto.erp.core.sales.product.element.ArticleDesignationSQLElement;
import org.openconcerto.erp.core.sales.product.element.ArticleTarifSQLElement;
import org.openconcerto.erp.core.sales.product.element.EcoContributionSQLElement;
import org.openconcerto.erp.core.sales.product.element.FamilleArticleSQLElement;
import org.openconcerto.erp.core.sales.product.element.FamilleEcoContributionSQLElement;
import org.openconcerto.erp.core.sales.product.element.MetriqueSQLElement;
import org.openconcerto.erp.core.sales.product.element.ModeVenteArticleSQLElement;
import org.openconcerto.erp.core.sales.product.element.ProductItemSQLElement;
import org.openconcerto.erp.core.sales.product.element.ProductQtyPriceSQLElement;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.product.element.ReliquatSQLElement;
import org.openconcerto.erp.core.sales.product.element.ReliquatSQLElement.ReliquatBRSQLElement;
import org.openconcerto.erp.core.sales.product.element.UniteVenteArticleSQLElement;
import org.openconcerto.erp.core.sales.quote.element.DevisItemSQLElement;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.core.sales.quote.element.EtatDevisSQLElement;
import org.openconcerto.erp.core.sales.quote.element.TransferQuoteSQLElement;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonItemSQLElement;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonSQLElement;
import org.openconcerto.erp.core.sales.shipment.element.TransferShipmentSQLElement;
import org.openconcerto.erp.core.supplychain.credit.element.AvoirFournisseurSQLElement;
import org.openconcerto.erp.core.supplychain.order.element.CommandeElementSQLElement;
import org.openconcerto.erp.core.supplychain.order.element.CommandeSQLElement;
import org.openconcerto.erp.core.supplychain.order.element.FactureFournisseurElementSQLElement;
import org.openconcerto.erp.core.supplychain.order.element.FactureFournisseurSQLElement;
import org.openconcerto.erp.core.supplychain.order.element.SaisieAchatSQLElement;
import org.openconcerto.erp.core.supplychain.order.element.TransferPurchaseSQLElement;
import org.openconcerto.erp.core.supplychain.order.element.TransferSupplierOrderSQLElement;
import org.openconcerto.erp.core.supplychain.product.element.ArticleFournisseurSQLElement;
import org.openconcerto.erp.core.supplychain.product.element.FamilleArticleFounisseurSQLElement;
import org.openconcerto.erp.core.supplychain.receipt.element.BonReceptionElementSQLElement;
import org.openconcerto.erp.core.supplychain.receipt.element.BonReceptionSQLElement;
import org.openconcerto.erp.core.supplychain.receipt.element.CodeFournisseurSQLElement;
import org.openconcerto.erp.core.supplychain.receipt.element.TransferReceiptSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.StockSQLElement;
import org.openconcerto.erp.core.supplychain.supplier.element.EcheanceFournisseurSQLElement;
import org.openconcerto.erp.core.supplychain.supplier.element.FournisseurSQLElement;
import org.openconcerto.erp.generationDoc.element.ModeleSQLElement;
import org.openconcerto.erp.generationDoc.element.TypeModeleSQLElement;
import org.openconcerto.erp.generationDoc.provider.AdresseFullClientValueProvider;
import org.openconcerto.erp.generationDoc.provider.AdresseRueClientValueProvider;
import org.openconcerto.erp.generationDoc.provider.AdresseVilleCPClientValueProvider;
import org.openconcerto.erp.generationDoc.provider.AdresseVilleClientValueProvider;
import org.openconcerto.erp.generationDoc.provider.AdresseVilleNomClientValueProvider;
import org.openconcerto.erp.generationDoc.provider.DateBLProvider;
import org.openconcerto.erp.generationDoc.provider.DateProvider;
import org.openconcerto.erp.generationDoc.provider.FacturableValueProvider;
import org.openconcerto.erp.generationDoc.provider.FormatedGlobalQtyTotalProvider;
import org.openconcerto.erp.generationDoc.provider.LabelAccountInvoiceProvider;
import org.openconcerto.erp.generationDoc.provider.MergedGlobalQtyTotalProvider;
import org.openconcerto.erp.generationDoc.provider.ModeDeReglementDetailsProvider;
import org.openconcerto.erp.generationDoc.provider.PaiementRemainedProvider;
import org.openconcerto.erp.generationDoc.provider.PrixUVProvider;
import org.openconcerto.erp.generationDoc.provider.PrixUnitaireProvider;
import org.openconcerto.erp.generationDoc.provider.PrixUnitaireRemiseProvider;
import org.openconcerto.erp.generationDoc.provider.QteTotalProvider;
import org.openconcerto.erp.generationDoc.provider.RecapFactureProvider;
import org.openconcerto.erp.generationDoc.provider.RefClientValueProvider;
import org.openconcerto.erp.generationDoc.provider.RemiseProvider;
import org.openconcerto.erp.generationDoc.provider.RemiseTotalProvider;
import org.openconcerto.erp.generationDoc.provider.RestantAReglerProvider;
import org.openconcerto.erp.generationDoc.provider.SaledTotalNotDiscountedProvider;
import org.openconcerto.erp.generationDoc.provider.StockLocationProvider;
import org.openconcerto.erp.generationDoc.provider.TotalAcompteProvider;
import org.openconcerto.erp.generationDoc.provider.TotalCommandeClientProvider;
import org.openconcerto.erp.generationDoc.provider.UserCreateInitialsValueProvider;
import org.openconcerto.erp.generationDoc.provider.UserCurrentInitialsValueProvider;
import org.openconcerto.erp.generationDoc.provider.UserModifyInitialsValueProvider;
import org.openconcerto.erp.generationEcritures.provider.SalesCreditAccountingRecordsProvider;
import org.openconcerto.erp.generationEcritures.provider.SalesInvoiceAccountingRecordsProvider;
import org.openconcerto.erp.generationEcritures.provider.SupplyOrderAccountingRecordsProvider;
import org.openconcerto.erp.injector.AchatAvoirSQLInjector;
import org.openconcerto.erp.injector.ArticleCommandeEltSQLInjector;
import org.openconcerto.erp.injector.BonFactureEltSQLInjector;
import org.openconcerto.erp.injector.BonFactureSQLInjector;
import org.openconcerto.erp.injector.BonReceptionFactureFournisseurSQLInjector;
import org.openconcerto.erp.injector.BrFactureAchatSQLInjector;
import org.openconcerto.erp.injector.CommandeBlEltSQLInjector;
import org.openconcerto.erp.injector.CommandeBlSQLInjector;
import org.openconcerto.erp.injector.CommandeBrSQLInjector;
import org.openconcerto.erp.injector.CommandeCliCommandeSQLInjector;
import org.openconcerto.erp.injector.CommandeFactureAchatSQLInjector;
import org.openconcerto.erp.injector.CommandeFactureClientSQLInjector;
import org.openconcerto.erp.injector.DevisCommandeFournisseurSQLInjector;
import org.openconcerto.erp.injector.DevisCommandeSQLInjector;
import org.openconcerto.erp.injector.DevisEltFactureEltSQLInjector;
import org.openconcerto.erp.injector.DevisFactureSQLInjector;
import org.openconcerto.erp.injector.EcheanceEncaisseSQLInjector;
import org.openconcerto.erp.injector.EcheanceRegleSQLInjector;
import org.openconcerto.erp.injector.FactureAvoirSQLInjector;
import org.openconcerto.erp.injector.FactureBonSQLInjector;
import org.openconcerto.erp.injector.FactureCommandeSQLInjector;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionClientPreferencePanel;
import org.openconcerto.erp.preferences.GestionCommercialeGlobalPreferencePanel;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.erp.storage.CloudStorageEngine;
import org.openconcerto.erp.storage.StorageEngines;
import org.jopendocument.link.OOConnexion;
import org.openconcerto.sql.ShowAs;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.element.SharedSQLElement;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBStructureItemNotFound;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.FieldMapper;
import org.openconcerto.sql.model.LoadingListener;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.task.TacheActionManager;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.task.element.FWKListPrefs;
import org.openconcerto.task.element.FWKSessionState;
import org.openconcerto.utils.BaseDirs;
import org.openconcerto.utils.DesktopEnvironment;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.NetUtils;
import org.openconcerto.utils.ProductInfo;
import org.openconcerto.utils.StringInputStream;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import eu.medsea.mimeutil.MimeUtil;
import eu.medsea.mimeutil.detector.ExtensionMimeDetector;
import eu.medsea.mimeutil.detector.MagicMimeMimeDetector;

/***************************************************************************************************
 * Configuration de la gestion: Une base commune "Common" --> société, user, tasks. Une base
 * par défaut pour créer une société "Default". Un fichier mapping.xml pour la base commune.
 * Un fichier mappingCompta.xml pour les bases sociétés.
 **************************************************************************************************/
// final so we can use setupLogging(), see the constructor comment
public final class ComptaPropsConfiguration extends ComptaBasePropsConfiguration {

    public static final ProductInfo productInfo = ProductInfo.getInstance();
    public static final String APP_NAME = productInfo.getName();
    private static final String DEFAULT_ROOT = "Common";

    static final Properties createDefaults() {
        final Properties defaults = new Properties();
        defaults.setProperty("base.root", DEFAULT_ROOT);
        return defaults;
    }

    // the properties path from this class
    private static final String PROPERTIES = "main.properties";

    public static final String DATA_DIR_VAR = "${data.dir}";

    // private Logger rootLogger;

    private String version = "";
    private static OOConnexion conn;

    public static OOConnexion getOOConnexion() {
        if (conn == null || conn.isClosed()) {
            try {
                conn = OOConnexion.create();

                if (conn == null) {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(null, "Impossible de trouver une installation d'OpenOffice sur votre ordinateur.\nMerci d'installer OpenOffice (http://fr.openoffice.org).");
                        }
                    });
                }

            } catch (IllegalStateException e) {
                JOptionPane.showMessageDialog(null, "Impossible d'obtenir une connexion avec openoffice. Contactez votre revendeur.");
                e.printStackTrace();
                conn = null;
            }
        }

        return conn;
    }

    public static void closeOOConnexion() {
        if (conn != null) {
            conn.closeConnexion();
        }
    }

    public static ComptaPropsConfiguration create() {
        return create(false);
    }

    public static ComptaPropsConfiguration create(final boolean nullAllowed) {
        return create(nullAllowed, getConfFile(productInfo));
    }

    public static ComptaPropsConfiguration create(final boolean nullAllowed, final File confFile) {
        // Log pour debug demarrage
        System.out.println("Loading configuration from: " + (confFile == null ? "null" : confFile.getAbsolutePath()));
        final boolean inWebStart = Gestion.inWebStart();
        final Properties defaults = createDefaults();
        // Ordre de recherche:
        // a/ fichier de configuration
        // b/ dans le jar
        try {
            final Properties props;
            // webstart should be self-contained, e.g. if a user launches from the web it shoudln't
            // read an old preference file but should always read its own configuration.
            if (confFile != null && confFile.exists() && !inWebStart) {
                props = create(new FileInputStream(confFile), defaults);
            } else {
                final InputStream stream = ComptaPropsConfiguration.class.getResourceAsStream(PROPERTIES);
                if (stream != null)
                    props = create(stream, defaults);
                else if (nullAllowed)
                    return null;
                else
                    throw new IOException("found neither " + confFile + " nor embedded " + PROPERTIES);
            }
            return new ComptaPropsConfiguration(props, inWebStart, true);
        } catch (final IOException e) {
            e.printStackTrace();
            String title = "Logiciel non configuré";
            String message = "Impossible de lire le fichier de configuration.\nIl est nécessaire d'utiliser le logiciel de Configuration pour paramétrer le logiciel.";
            JOptionPane.showMessageDialog(new JFrame(), message, title, JOptionPane.ERROR_MESSAGE);
            System.exit(2);
            // never reached since we're already dead
            return null;
        }

    }

    // *** instance

    private final boolean isMain;
    private final boolean inWebstart;
    private final boolean isServerless;
    private boolean isOnCloud;
    private boolean isPortable;
    private File portableDir = null;
    private Currency currency = null;

    // isMain=true also set up some VM wide settings
    public ComptaPropsConfiguration(Properties props, final boolean inWebstart, final boolean main) {
        super(props, productInfo);
        this.isMain = main;
        this.inWebstart = inWebstart;
        this.isPortable = Boolean.parseBoolean(this.getProperty("portable", "false"));
        String pDir = this.getProperty("portableDir", null);
        if (isPortable) {
            if (pDir == null) {
                System.out.println("Portable mode, using current directory");
                portableDir = new File(".");
            } else {
                System.out.println("Portable mode, using provided directory : " + pDir);
                portableDir = new File(pDir);
            }
            if (!portableDir.exists()) {
                System.out.println("Error: portable dir missing : " + portableDir);
                portableDir = new File(".");
            }
            System.out.println("Portable mode in : " + portableDir.getAbsolutePath());
            if (this.getProperty("portableDirMessage", "true").equalsIgnoreCase("true")) {
                if (!GraphicsEnvironment.isHeadless()) {
                    System.out.println("Add portableDirMessage=false in your " + PROPERTIES + " to prevent the popup message.");
                    JOptionPane.showMessageDialog(null, "Portable version :\n" + portableDir.getAbsolutePath());
                }
            }
        }
        if (isPortable) {
            this.setProperty("wd", new File(portableDir, "UserData").getAbsolutePath());
        } else {
            this.setProperty("wd", DesktopEnvironment.getDE().getDocumentsFolder().getAbsolutePath() + File.separator + this.getAppName());
        }
        if (this.getProperty("version.date") != null) {
            this.version = this.getProperty("version.date");
        }

        //
        String token = getToken();
        if (token != null) {
            this.isServerless = false;
            this.isOnCloud = true;
            if (this.getProperty("storage.server") == null) {
                InProgressFrame progress = new InProgressFrame();
                progress.show("Connexion sécurisée au cloud en cours");
                String result = NetUtils.getHTTPContent("https://cloud.openconcerto.org/getAuthInfo?token=" + token, false);
                if (result != null && !result.contains("ERROR")) {
                    Properties cProperty = new Properties();
                    try {
                        cProperty.loadFromXML(new StringInputStream(result));
                        setProperty("server.wan.only", "true");
                        setProperty("server.wan.port", "22");
                        // SSH
                        setProperty("server.wan.addr", cProperty.getProperty("ssh.server"));
                        setProperty("server.wan.user", cProperty.getProperty("ssh.login"));
                        setProperty("server.wan.password", cProperty.getProperty("ssh.pass"));
                        // DB
                        setProperty("server.ip", "127.0.0.1:5432");
                        setProperty("server.driver", "postgresql");
                        setProperty("server.login", cProperty.getProperty("db.login"));
                        setProperty("server.password", cProperty.getProperty("db.pass"));
                        setProperty("systemRoot", cProperty.getProperty("db.name"));
                        // Storage
                        props.put("storage.server", cProperty.getProperty("storage.server"));

                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(new JFrame(), "Impossible récupérer les informations de connexion");
                        System.exit(1);
                    }

                } else if (result != null && result.contains("not paid")) {
                    JOptionPane.showMessageDialog(new JFrame(), "Compte Cloud non crédité");
                    System.exit(1);
                } else {
                    JOptionPane.showMessageDialog(new JFrame(), "Connexion impossible au Cloud");
                    System.exit(1);
                }
                progress.dispose();
            }
            StorageEngines.getInstance().addEngine(new CloudStorageEngine());
        } else {
            // FIXME
            // Local database
            {
                if (getProperty("server.login") == null) {
                    setProperty("server.login", "openconcerto");
                }
                if (getProperty("server.password") == null) {
                    setProperty("server.password", "openconcerto");
                }
            }
            this.setProperty("server.ip", getProperty("server.ip").replace(DATA_DIR_VAR, getDataDir().getPath()));
            final SQLSystem system = getSystem();
            this.isServerless = system == SQLSystem.H2 && system.getHostname(getServerIp()) == null;
        }
        if (this.isMain) {
            // ATTN this works because this is executed last (i.e. if you put this in a superclass
            // this won't work since e.g. app.name won't have its correct value)
            try {
                this.setupLogging("logs");
            } catch (Exception e) {
                System.err.println("ComptaPropsConfiguration() error in log setup : " + e.getMessage());
            }
            registerAccountingProvider();
            registerCellValueProvider();
        }

        MimeUtil.registerMimeDetector(ExtensionMimeDetector.class.getName());
        MimeUtil.registerMimeDetector(MagicMimeMimeDetector.class.getName());
    }

    private void registerAccountingProvider() {
        SalesInvoiceAccountingRecordsProvider.register();
        SalesCreditAccountingRecordsProvider.register();
        SupplyOrderAccountingRecordsProvider.register();
    }

    private void registerCellValueProvider() {
        UserCreateInitialsValueProvider.register();
        UserModifyInitialsValueProvider.register();
        UserCurrentInitialsValueProvider.register();
        PrixUnitaireRemiseProvider.register();
        PrixUnitaireProvider.register();
        PrixUVProvider.register();
        TotalAcompteProvider.register();
        FacturableValueProvider.register();
        TotalCommandeClientProvider.register();
        LabelAccountInvoiceProvider.register();
        DateBLProvider.register();
        AdresseRueClientValueProvider.register();
        AdresseVilleClientValueProvider.register();
        AdresseVilleCPClientValueProvider.register();
        AdresseVilleNomClientValueProvider.register();
        AdresseFullClientValueProvider.register();
        QteTotalProvider.register();
        StockLocationProvider.register();
        RefClientValueProvider.register();
        ModeDeReglementDetailsProvider.register();
        FormatedGlobalQtyTotalProvider.register();
        MergedGlobalQtyTotalProvider.register();
        PaiementRemainedProvider.register();
        RemiseProvider.register();
        DateProvider.register();
        RemiseTotalProvider.register();
        RecapFactureProvider.register();
        RestantAReglerProvider.register();
        SaledTotalNotDiscountedProvider.register();
    }

    @Override
    protected void initSystemRoot(DBSystemRoot input) {
        super.initSystemRoot(input);
        if (!GraphicsEnvironment.isHeadless()) {
            final JDialog f = new JOptionPane("Mise à jour des caches en cours...\nCette opération prend généralement moins d'une minute.", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                    null, new Object[] {}).createDialog("Veuillez patienter");
            input.addLoadingListener(new LoadingListener() {

                private int loadingCount = 0;
                private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        final Thread thread = new Thread(r, "Loading listener thread");
                        thread.setDaemon(true);
                        return thread;
                    }
                });
                private ScheduledFuture<?> future = null;

                @Override
                public synchronized void loading(LoadingEvent evt) {
                    this.loadingCount += evt.isStarting() ? 1 : -1;
                    if (this.loadingCount < 0) {
                        throw new IllegalStateException();
                    } else if (this.loadingCount == 0) {
                        this.future.cancel(false);
                        this.future = null;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                f.setVisible(false);
                                f.dispose();
                            }
                        });
                    } else if (this.future == null) {
                        this.future = this.exec.schedule(new Runnable() {
                            @Override
                            public void run() {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        f.setVisible(true);
                                    }
                                });
                            }
                        }, 1, TimeUnit.SECONDS);
                    }
                }

            });
        }
    }

    @Override
    protected void initDS(SQLDataSource ds) {
        super.initDS(ds);
        ds.setInitialSize(3);
        ds.setMinIdle(2);
        ds.setMaxActive(4);
    }

    public String getToken() {
        return getProperty("token");
    }

    @Override
    public void destroy() {
        // since we used setupLogging() in the constructor (allows to remove confDir)
        if (this.isMain) {
            this.tearDownLogging(true);
        }
        super.destroy();
    }

    public final boolean isServerless() {
        return this.isServerless;
    }

    public final boolean isOnCloud() {
        return this.isOnCloud;
    }

    public final String getVersion() {
        return this.version;
    }

    @Override
    public String getAppVariant() {
        if (inWebstart())
            // so we don't remove files of a normal GestionNX
            return super.getAppVariant() + "-webstart";
        else
            return super.getAppVariant();
    }

    @Override
    protected BaseDirs createBaseDirs() {
        if (isPortable()) {
            return BaseDirs.createPortable(getPortableDir(), this.getProductInfo(), this.getAppVariant());
        } else {
            return super.createBaseDirs();
        }
    }

    @Override
    protected final File getOldConfDir() {
        if (isPortable) {
            return getWD();
        } else {
            return Gestion.MAC_OS_X ? new File(System.getProperty("user.home") + "/Library/Application Support/" + getAppID()) : super.getOldConfDir();
        }
    }

    public boolean isPortable() {
        return isPortable;
    }

    public File getPortableDir() {
        return portableDir;
    }

    private boolean inWebstart() {
        return this.inWebstart;
    }

    @Deprecated
    public File getOldDataDir() {
        return new File(this.getOldConfDir(), "DBData");
    }

    public File getDataDir() {
        return new File(this.getBaseDirs().getAppDataFolder(), "DBData");
    }

    private final void createDB(final DBSystemRoot sysRoot) {
        if (!this.isServerless())
            return;
        try {
            // H2 create the database on connection
            // don't create if root explicitly excluded (e.g. map no roots just to quickly test
            // connection)
            if (sysRoot.shouldMap(getRootName()) && !sysRoot.contains(getRootName())) {
                Log.get().warning("Creating DB");
                String createScript = null;
                try {
                    createScript = this.getResource("/webstart/create.sql");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (createScript == null)
                    throw new IllegalStateException("Couldn't find database creation script");
                final SQLDataSource ds = sysRoot.getDataSource();
                ds.execute("RUNSCRIPT from '" + createScript + "' CHARSET 'UTF-8' ;");
                sysRoot.refetch();
                this.setupSystemRoot(sysRoot);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't create database", e);
        }
    }

    @Override
    protected DBSystemRoot createSystemRoot() {
        final DBSystemRoot res = super.createSystemRoot();
        // Don't create a separate server for createDB() as on normal databases just setting up a
        // data source can take 2 seconds (e.g. validateConnectionFactory()). And this is for every
        // boot.
        this.createDB(res);
        return res;
    }

    @Override
    public String getDefaultBase() {
        return super.getDefaultBase();

    }

    protected File getMappingFile() {
        return new File("mapping.xml");
    }

    @Override
    protected SQLElementDirectory createDirectory() {
        final SQLElementDirectory dir = super.createDirectory();
        dir.addSQLElement(new AdresseCommonSQLElement());
        dir.addSQLElement(new ExerciceCommonSQLElement());
        dir.addSQLElement(DeviseSQLElement.class);
        dir.addSQLElement(TypeModeleSQLElement.class);
        dir.addSQLElement(new SocieteCommonSQLElement());

        // DSN
        dir.addSQLElement(CaisseCotisationRenseignentSQLElement.class);
        dir.addSQLElement(CodeBaseAssujettieSQLElement.class);
        dir.addSQLElement(ContratModaliteTempsSQLElement.class);
        dir.addSQLElement(CodeCaisseTypeRubriqueSQLElement.class);
        dir.addSQLElement(CodeTypeRubriqueBrutSQLElement.class);
        dir.addSQLElement(MotifArretTravailSQLElement.class);
        dir.addSQLElement(ContratDispositifPolitiqueSQLElement.class);
        dir.addSQLElement(ContratDetacheExpatrieSQLElement.class);
        dir.addSQLElement(ContratRegimeMaladieSQLElement.class);
        dir.addSQLElement(ContratMotifRecoursSQLElement.class);
        dir.addSQLElement(ContratRegimeVieillesseSQLElement.class);
        dir.addSQLElement(MotifFinContratSQLElement.class);
        dir.addSQLElement(MotifRepriseArretTravailSQLElement.class);
        dir.addSQLElement(TypePreavisSQLElement.class);
        dir.addSQLElement(DSNNatureSQLElement.class);

        // ECO
        dir.addSQLElement(FamilleEcoContributionSQLElement.class);
        dir.addSQLElement(EcoContributionSQLElement.class);

        return dir;
    }

    private void setSocieteDirectory() {
        try {
            SQLElementDirectory dir = this.getDirectory();

            dir.addSQLElement(AttachmentSQLElement.class);

            dir.addSQLElement(ArticleTarifSQLElement.class);
            dir.addSQLElement(ReliquatBRSQLElement.class);
            dir.addSQLElement(ReliquatSQLElement.class);
            dir.addSQLElement(ProductQtyPriceSQLElement.class);
            dir.addSQLElement(ProductItemSQLElement.class);
            dir.addSQLElement(ArticleDesignationSQLElement.class);
            dir.addSQLElement(BanqueSQLElement.class);
            dir.addSQLElement(ClientDepartementSQLElement.class);
            dir.addSQLElement(CoefficientPrimeSQLElement.class);
            dir.addSQLElement(ContactFournisseurSQLElement.class);
            dir.addSQLElement(ContactAdministratifSQLElement.class);
            dir.addSQLElement(new TitrePersonnelSQLElement());
            dir.addSQLElement(new ContactSQLElement());
            dir.addSQLElement(new SaisieKmItemSQLElement());
            dir.addSQLElement(new EcritureSQLElement());

            dir.addSQLElement(new SharedSQLElement("EMPLOYEUR_MULTIPLE"));
            dir.addSQLElement(PosteAnalytiqueSQLElement.class);
            dir.addSQLElement(new SharedSQLElement("CLASSE_COMPTE"));

            dir.addSQLElement(new CaisseCotisationSQLElement());
            dir.addSQLElement(CaisseTicketSQLElement.class);

            dir.addSQLElement(new ImpressionRubriqueSQLElement());

            dir.addSQLElement(ModeleSQLElement.class);

            dir.addSQLElement(new ProfilPayeSQLElement());
            dir.addSQLElement(new ProfilPayeElementSQLElement());
            dir.addSQLElement(new PeriodeValiditeSQLElement());

            dir.addSQLElement(new RubriqueCotisationSQLElement());
            dir.addSQLElement(new RubriqueCommSQLElement());
            dir.addSQLElement(new RubriqueNetSQLElement());
            dir.addSQLElement(new RubriqueBrutSQLElement());

            dir.addSQLElement(new TypeRubriqueBrutSQLElement());
            dir.addSQLElement(new TypeRubriqueNetSQLElement());

            dir.addSQLElement(new VariablePayeSQLElement());

            dir.addSQLElement(new AdresseSQLElement());

            dir.addSQLElement(ReferenceArticleSQLElement.class);
            dir.addSQLElement(ArticleFournisseurSQLElement.class);
            dir.addSQLElement(FamilleArticleFounisseurSQLElement.class);

            dir.addSQLElement(new AssociationCompteAnalytiqueSQLElement());
            dir.addSQLElement(new AvoirClientSQLElement());
            dir.addSQLElement(new AvoirClientElementSQLElement());
            dir.addSQLElement(AvoirFournisseurSQLElement.class);
            dir.addSQLElement(new AcompteSQLElement());

            dir.addSQLElement(new AxeAnalytiqueSQLElement());

            dir.addSQLElement(new BonDeLivraisonItemSQLElement());
            dir.addSQLElement(new BonDeLivraisonSQLElement());
            dir.addSQLElement(new TransferShipmentSQLElement());

            dir.addSQLElement(new BonReceptionElementSQLElement());
            dir.addSQLElement(new BonReceptionSQLElement());
            dir.addSQLElement(new TransferReceiptSQLElement());
            dir.addSQLElement(new ChequeAEncaisserSQLElement());
            dir.addSQLElement(new ChequeAvoirClientSQLElement());
            dir.addSQLElement(new ChequeFournisseurSQLElement());
                dir.addSQLElement(new CustomerCategorySQLElement());
                dir.addSQLElement(new CustomerSQLElement());
                dir.addSQLElement(new CompteClientTransactionSQLELement());
            dir.addSQLElement(new CourrierClientSQLElement());

            dir.addSQLElement(new ClassementConventionnelSQLElement());
            dir.addSQLElement(CodeFournisseurSQLElement.class);
            dir.addSQLElement(new CommandeSQLElement());
            dir.addSQLElement(new TransferSupplierOrderSQLElement());
            dir.addSQLElement(new CommandeElementSQLElement());
            dir.addSQLElement(new TransferCustomerOrderSQLElement());
            dir.addSQLElement(new CommandeClientSQLElement());
            dir.addSQLElement(new CommandeClientElementSQLElement());

                dir.addSQLElement(new CommercialSQLElement());
            dir.addSQLElement(ObjectifSQLElement.class);
            dir.addSQLElement(new ComptePCESQLElement());
            dir.addSQLElement(new ComptePCGSQLElement());

            dir.addSQLElement(new ContratSalarieSQLElement());
            dir.addSQLElement(ContratPrevoyanceSQLElement.class);
            dir.addSQLElement(ContratPrevoyanceRubriqueSQLElement.class);
            dir.addSQLElement(ContratPrevoyanceRubriqueNetSQLElement.class);
            dir.addSQLElement(ContratPrevoyanceSalarieSQLElement.class);
            dir.addSQLElement(AyantDroitSQLElement.class);
            dir.addSQLElement(AyantDroitTypeSQLElement.class);
            dir.addSQLElement(AyantDroitContratPrevSQLElement.class);

            dir.addSQLElement(new CodeRegimeSQLElement());
            dir.addSQLElement(new CodeEmploiSQLElement());
            dir.addSQLElement(new CodeContratTravailSQLElement());
            dir.addSQLElement(new CodeDroitContratSQLElement());
            dir.addSQLElement(new CodeCaractActiviteSQLElement());

            dir.addSQLElement(new CodeStatutCategorielSQLElement());
            dir.addSQLElement(CodeStatutCategorielConventionnelSQLElement.class);
            dir.addSQLElement(new CodeStatutProfSQLElement());
            dir.addSQLElement(CaisseModePaiementSQLElement.class);
            dir.addSQLElement(CodeCotisationIndividuelleSQLElement.class);
            dir.addSQLElement(CodeCotisationEtablissementSQLElement.class);
            dir.addSQLElement(CodePenibiliteSQLElement.class);
            dir.addSQLElement(CodePenibiliteContratSQLElement.class);
            dir.addSQLElement(TypeComposantBaseAssujettieSQLElement.class);

            dir.addSQLElement(new CumulsCongesSQLElement());
            dir.addSQLElement(new CumulsPayeSQLElement());

            dir.addSQLElement(new DepartementSQLElement());
                dir.addSQLElement(new DevisSQLElement());
            dir.addSQLElement(new TransferQuoteSQLElement());
            dir.addSQLElement(new DevisItemSQLElement());

            dir.addSQLElement(new EcheanceClientSQLElement());
            dir.addSQLElement(new EcheanceFournisseurSQLElement());
            dir.addSQLElement(EncaisserMontantSQLElement.class);
            dir.addSQLElement(EncaisserMontantElementSQLElement.class);
            dir.addSQLElement(EcoTaxeSQLElement.class);

            dir.addSQLElement(new EtatCivilSQLElement());
            dir.addSQLElement(new EtatDevisSQLElement());

            dir.addSQLElement(new FamilleArticleSQLElement());
            dir.addSQLElement(new FichePayeSQLElement());
            dir.addSQLElement(new FichePayeElementSQLElement());

            dir.addSQLElement(new FournisseurSQLElement());

            dir.addSQLElement(new CodeIdccSQLElement());

            dir.addSQLElement(new InfosSalariePayeSQLElement());

            dir.addSQLElement(new JournalSQLElement());

            dir.addSQLElement(LangueSQLElement.class);

            dir.addSQLElement(new MetriqueSQLElement());
            dir.addSQLElement(new ModeleCourrierClientSQLElement());
            dir.addSQLElement(new ModeVenteArticleSQLElement());
            dir.addSQLElement(new ModeDeReglementSQLElement());
            dir.addSQLElement(new ModeReglementPayeSQLElement());
            dir.addSQLElement(new MoisSQLElement());
            dir.addSQLElement(new MouvementSQLElement());
            dir.addSQLElement(new MouvementStockSQLElement());

            dir.addSQLElement(new NatureCompteSQLElement());

            dir.addSQLElement(new NumerotationAutoSQLElement());

            dir.addSQLElement(new PaysSQLElement());

            dir.addSQLElement(new PieceSQLElement());

            dir.addSQLElement(new ProfilPayeElementSQLElement());

            dir.addSQLElement(ReferenceClientSQLElement.class);
            dir.addSQLElement(new RegimeBaseSQLElement());
            dir.addSQLElement(new RelanceSQLElement());
            dir.addSQLElement(new ReglementPayeSQLElement());
            dir.addSQLElement(new ReglerMontantSQLElement());
            dir.addSQLElement(ReglerMontantElementSQLElement.class);
            dir.addSQLElement(RepartitionAnalytiqueSQLElement.class);

            dir.addSQLElement(new SaisieAchatSQLElement());
            dir.addSQLElement(new FactureFournisseurSQLElement());
            dir.addSQLElement(new FactureFournisseurElementSQLElement());
            dir.addSQLElement(new TransferPurchaseSQLElement());
            dir.addSQLElement(new SaisieKmSQLElement());
            dir.addSQLElement(new SaisieVenteComptoirSQLElement());
            dir.addSQLElement(new SaisieVenteFactureSQLElement());
            dir.addSQLElement(new TransferInvoiceSQLElement());
            // at the end since it specifies action which initialize foreign keys
            dir.addSQLElement(AssociationAnalytiqueSQLElement.class);
                dir.addSQLElement(new SaisieVenteFactureItemSQLElement());

            dir.addSQLElement(SituationFamilialeSQLElement.class);
            dir.addSQLElement(new StockSQLElement());
            dir.addSQLElement(new StyleSQLElement());

            dir.addSQLElement(new SalarieSQLElement());

            dir.addSQLElement(TarifSQLElement.class);
            dir.addSQLElement(new TaxeSQLElement());
            dir.addSQLElement(TaxeComplementaireSQLElement.class);
            dir.addSQLElement(TicketCaisseSQLElement.class);

            dir.addSQLElement(new TypeComptePCGSQLElement());
            dir.addSQLElement(new TypeLettreRelanceSQLElement());
            dir.addSQLElement(new TypeReglementSQLElement());

            dir.addSQLElement(new VariableSalarieSQLElement());
            dir.addSQLElement(UniteVenteArticleSQLElement.class);

            dir.addSQLElement(CalendarItemSQLElement.class);
            dir.addSQLElement(CalendarItemGroupSQLElement.class);
            dir.addSQLElement(DeviseHistoriqueSQLElement.class);

            if (getRootSociete().contains("FWK_LIST_PREFS")) {
                dir.addSQLElement(new FWKListPrefs(getRootSociete()));
            }
            if (getRootSociete().contains("FWK_SESSION_STATE")) {
                dir.addSQLElement(new FWKSessionState(getRootSociete()));
            }

            // check that all codes are unique
            Collection<SQLElement> elements = dir.getElements();
            String s = "";
            for (SQLElement sqlElement : elements) {
                try {
                    SQLElement e = dir.getElementForCode(sqlElement.getCode());
                    if (e != sqlElement) {
                        s += "Error while retrieving element from code " + sqlElement.getCode() + "\n";
                    }
                } catch (Throwable e) {
                    s += "Error while retrieving element from code " + sqlElement.getCode() + " :\n " + e.getMessage() + "\n";
                }
            }
            if (!s.trim().isEmpty()) {
                ExceptionHandler.handle(s);
            }
        } catch (DBStructureItemNotFound e) {
            JOptionPane.showMessageDialog(null,
                    "Une table ou un champ est manquant dans la base de données. Mettez à jour votre base de données via l'outil de configuration si vous venez de changer de version d'Openconcerto.");
            throw e;
        }
    }

    private void setSocieteSQLInjector() {
        final DBRoot rootSociete = getRootSociete();
        setSocieteSQLInjector(rootSociete);

    }

    public static void setSocieteSQLInjector(final DBRoot rootSociete) {
        new AchatAvoirSQLInjector(rootSociete);
        new ArticleCommandeEltSQLInjector(rootSociete);
        new CommandeCliCommandeSQLInjector(rootSociete);
        new FactureAvoirSQLInjector(rootSociete);
        new FactureBonSQLInjector(rootSociete);
        new FactureCommandeSQLInjector(rootSociete);
        new DevisFactureSQLInjector(rootSociete);
        new DevisCommandeSQLInjector(rootSociete);
        new DevisCommandeFournisseurSQLInjector(rootSociete);
        new CommandeBlEltSQLInjector(rootSociete);
        new CommandeBlSQLInjector(rootSociete);
        new BonFactureSQLInjector(rootSociete);
        new BonFactureEltSQLInjector(rootSociete);
        new CommandeFactureClientSQLInjector(rootSociete);
        new CommandeBrSQLInjector(rootSociete);
        new BonReceptionFactureFournisseurSQLInjector(rootSociete);
        new CommandeFactureAchatSQLInjector(rootSociete);
        new EcheanceEncaisseSQLInjector(rootSociete);
        new EcheanceRegleSQLInjector(rootSociete);
        new BrFactureAchatSQLInjector(rootSociete);
        new DevisEltFactureEltSQLInjector(rootSociete);
    }


    private void setSocieteShowAs() {
        final ShowAs showAs = this.getShowAs();
        final DBRoot root = this.getRootSociete();
        showAs.setRoot(getRootSociete());

        List<String> listAdrShowAs = SQLRow.toList("RUE,CODE_POSTAL,VILLE");
        if (root.contains("ADRESSE") && root.getTable("ADRESSE").contains("DISTRICT")) {
            listAdrShowAs = SQLRow.toList("RUE,DISTRICT,DEPARTEMENT,CODE_POSTAL,VILLE");
        }
        showAs.show("ADRESSE", listAdrShowAs);

        showAs.show("AXE_ANALYTIQUE", "NOM");

        List<String> lEcr = new ArrayList<String>();

        lEcr.add("ID_MOUVEMENT");
        lEcr.add("ID_JOURNAL");
        lEcr.add("ID_COMPTE_PCE");
        lEcr.add("DATE");

        showAs.show(root.getTable("ASSOCIATION_ANALYTIQUE").getField("ID_ECRITURE"), lEcr);

        showAs.show("CHEQUE_A_ENCAISSER", "MONTANT", "ID_CLIENT");

                if (getRootSociete().getTable("CLIENT").getFieldsName().contains("LOCALISATION")) {
                    showAs.show("CLIENT", "NOM", "LOCALISATION");
                } else {
                    SQLPreferences prefs = new SQLPreferences(root);
                    if (prefs.getBoolean(GestionClientPreferencePanel.DISPLAY_CLIENT_PCE, false)) {
                        showAs.show("CLIENT", "ID_PAYS", "GROUPE", "NOM", "ID_COMPTE_PCE");
                    } else {
                        showAs.show("CLIENT", "ID_PAYS", "GROUPE", "NOM");
                    }
                }

        showAs.show(BanqueSQLElement.TABLENAME, "NOM");

        showAs.show("CLASSEMENT_CONVENTIONNEL", "NIVEAU", "COEFF");
        showAs.show("CODE_EMPLOI", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_CONTRAT_TRAVAIL", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_DROIT_CONTRAT", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_CARACT_ACTIVITE", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_STATUT_PROF", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_STATUT_CATEGORIEL", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_REGIME", SQLRow.toList("CODE,NOM"));
        showAs.show("COMMANDE", "NOM");
        showAs.show("COMMANDE_CLIENT", "NOM", "T_HT");
        showAs.show("COMPTE_PCE", "NUMERO", "NOM");
        showAs.show("COMPTE_PCG", "NUMERO", "NOM");
        showAs.show("CONTACT", "NOM");
        showAs.show("CONTRAT_SALARIE", "NATURE");

        List<String> listFieldDevisElt = new ArrayList<String>();
        listFieldDevisElt.add("NUMERO");
        listFieldDevisElt.add("DATE");
        listFieldDevisElt.add("ID_CLIENT");

        listFieldDevisElt.add("ID_ETAT_DEVIS");
        showAs.showField("DEVIS_ELEMENT.ID_DEVIS", listFieldDevisElt);

        showAs.show("DEPARTEMENT", "NUMERO", "NOM");

        showAs.show("ECRITURE", SQLRow.toList("NOM,DATE,ID_COMPTE_PCE,DEBIT,CREDIT"));
        showAs.show("ECHEANCE_CLIENT", SQLRow.toList("ID_CLIENT,ID_MOUVEMENT"));
        final List<String> lEchFact = new ArrayList<String>();
        lEchFact.add("NUMERO");
        lEchFact.add("DATE");
        SQLTable tableEch = root.getTable("ECHEANCE_CLIENT");
        showAs.show(tableEch.getField("ID_SAISIE_VENTE_FACTURE"), lEchFact);

        showAs.show("ECHEANCE_FOURNISSEUR", SQLRow.toList("ID_FOURNISSEUR,ID_MOUVEMENT"));
        showAs.show("FICHE_PAYE", SQLRow.toList("ID_MOIS,ANNEE"));
        showAs.show("FOURNISSEUR", "NOM");

        showAs.show("IDCC", "NOM");

        showAs.show("JOURNAL", "NOM");
        showAs.show("MOIS", "NOM");
        showAs.show("MOUVEMENT", "NUMERO", "ID_PIECE");
        showAs.show("MODE_VENTE_ARTICLE", "NOM");
        showAs.show("MODE_REGLEMENT", "ID_TYPE_REGLEMENT", "AJOURS");
        showAs.show("MODE_REGLEMENT_PAYE", "NOM");
        showAs.show("MODELE_COURRIER_CLIENT", "NOM", "CONTENU");

        showAs.show("NATURE_COMPTE", "NOM");
        showAs.show("POSTE_ANALYTIQUE", "NOM", "ID_AXE_ANALYTIQUE");
        showAs.show("PAYS", "CODE", "NOM");
        showAs.show("PIECE", "ID", "NOM");

        final SQLElementDirectory directory = this.getDirectory();
        showAs.show("REPARTITION_ANALYTIQUE", "NOM");
        showAs.show("REGIME_BASE", "ID_CODE_REGIME_BASE");
        showAs.show("REGLEMENT_PAYE", "NOM_BANQUE", "RIB");

        List<String> listFieldModReglMontant = new ArrayList<String>();
        listFieldModReglMontant.add("ID_TYPE_REGLEMENT");

        showAs.showField("REGLER_MONTANT.ID_MODE_REGLEMENT", listFieldModReglMontant);
        showAs.showField("ENCAISSER_MONTANT.ID_MODE_REGLEMENT", listFieldModReglMontant);

        List<String> listFieldFactureElt = new ArrayList<String>();
        listFieldFactureElt.add("NUMERO");
        listFieldFactureElt.add("DATE");
        listFieldFactureElt.add("ID_CLIENT");
        showAs.showField("SAISIE_VENTE_FACTURE_ELEMENT.ID_SAISIE_VENTE_FACTURE", listFieldFactureElt);

        showAs.show("SALARIE", SQLRow.toList("CODE,NOM,PRENOM"));

        showAs.show("SITUATION_FAMILIALE", "NOM");

        showAs.show("STYLE", "NOM");

        showAs.show("TAXE", "TAUX");

        showAs.show(directory.getElement("TITRE_PERSONNEL").getTable(), asList("NOM"));

        showAs.show("TYPE_COMPTE_PCG", "NOM");
        showAs.show("TYPE_LETTRE_RELANCE", "NOM");
        showAs.show("TYPE_REGLEMENT", "NOM");

    }

    public String setUpSocieteStructure(int base) {
        setRowSociete(base);

        // find customer
        String customerName = "openconcerto";
        final String dbMD = getRootSociete().getMetadata("CUSTOMER");
        if (dbMD != null && !dbMD.equals(customerName))
            throw new IllegalStateException("customer is '" + customerName + "' but db says '" + dbMD + "'");
        return customerName;
    }

    @Override
    public void setUpSocieteDataBaseConnexion(int base) {
        final String customerName = setUpSocieteStructure(base);
        final DBRoot rootSociete = this.getRootSociete();
        closeSocieteConnexion();
        setSocieteDirectory();
        NumerotationAutoSQLElement.addListeners();
        loadTranslations(this.getTranslator(), rootSociete, Arrays.asList("mappingCompta", "mapping-" + customerName));
        setSocieteShowAs();
        setSocieteSQLInjector();
        configureGlobalMapper();
        setFieldMapper(new FieldMapper(this.getRootSociete()));
        getFieldMapper().addMapperStreamFromClass(Gestion.class);
        TemplateNXProps.getInstance();
        // Prefetch undefined
        rootSociete.getTables().iterator().next().getUndefinedID();
        SQLPreferences pref = new SQLPreferences(rootSociete);
        if (pref.getBoolean(GestionCommercialeGlobalPreferencePanel.BARCODE_INSERTION, false)) {
            this.barcodeReader = new BarcodeReader(80);
            this.barcodeReader.start();
        }
    }

    private BarcodeReader barcodeReader = null;

    public BarcodeReader getBarcodeReader() {
        return barcodeReader;
    }

    private void configureGlobalMapper() {

        FieldMapper fieldMapper = new FieldMapper(this.getRootSociete());
        fieldMapper.addMapperStreamFromClass(Gestion.class);

    }

    private void closeSocieteConnexion() {

    }

    public String getServerIp() {
        return getProperty("server.ip");
    }

    @Override
    protected DateFormat getLogDateFormat() {
        return new SimpleDateFormat("yyyy-MM/dd_HH-mm EEEE");
    }

    @Override
    protected SQLServer createServer() {
        if (GraphicsEnvironment.isHeadless()) {
            SQLServer server = super.createServer();
            return server;
        }
        InProgressFrame progress = new InProgressFrame();
        progress.show("Connexion à votre base de données en cours");
        try {
            SQLServer server = super.createServer();
            return server;
        } catch (Throwable e) {
            ExceptionHandler.die("Impossible de se connecter à la base de données.\nVérifiez votre connexion.", e);
            return null;
        } finally {
            progress.dispose();
        }

    }

    public static ComptaPropsConfiguration getInstanceCompta() {
        return (ComptaPropsConfiguration) getInstance();
    }

    public String getStorageServer() {
        return this.getProperty("storage.server");
    }

    public Image getCustomLogo() {
        final File dir = new File(getConfFile(getProductInfo()).getParent());
        final File file = new File(dir, "logo.png");

        BufferedImage im = null;
        if (file.exists()) {
            try {
                im = ImageIO.read(file);
                if (im.getHeight() < 16) {
                    JOptionPane.showMessageDialog(new JFrame(), "Logo too small (height < 16 pixels)");
                    return null;
                }
                if (im.getWidth() < 200) {
                    JOptionPane.showMessageDialog(new JFrame(), "Logo too small (width < 200 pixels)");
                    return null;
                }
                final Graphics g = im.getGraphics();

                g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                final String str = "Powered by OpenConcerto";
                final Rectangle2D r = g.getFontMetrics().getStringBounds(str, g);
                g.setColor(new Color(255, 255, 255, 200));
                g.fillRect(0, im.getHeight() - (int) r.getHeight() - 2, (int) r.getWidth() + 8, (int) r.getHeight() + 4);
                g.setColor(Color.BLACK);
                g.drawString(str, 4, im.getHeight() - 4);
                g.dispose();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return im;
    }

    public synchronized Currency getCurrency() {
        if (currency == null) {
            String code = getRowSociete().getForeign("ID_DEVISE").getString("CODE");
            currency = new Currency(code);
        }
        return currency;
    }
}
