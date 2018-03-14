package controller;


import dbConnection.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jsonvalidator.apiclient.APIClient;
import jsonvalidator.mapper.CardAssignment;
import jsonvalidator.mapper.EligibleList;
import jsonvalidator.mapper.EncryptedSHR;
import jsonvalidator.mapper.SHR;
import jsonvalidator.utils.Compression;
import jsonvalidator.utils.Encryption;
import jsonvalidator.utils.SHRUtils;
import models.*;
import pSmart.MainSmartCardReadWrite;
import pSmart.SmartCardUtils;
import com.jfoenix.controls.JFXComboBox;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import view.Main;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class HomeController  {

    private String message=null;
    MainSmartCardReadWrite readerWriter;

    @FXML
    private Label lblpsmartTitle;

    @FXML
    private TabPane tpMainTabPane;

    @FXML
    private Button btnWriteToCard;

    @FXML
    private Label lblFacilityName;

    @FXML
    private TextArea txtProcessLogger;

    @FXML
    private TableColumn<CardDetail, String> colCardStatus;

    @FXML
    private TableColumn<CardDetail, String> colReason;

    @FXML
    private TableColumn<CardDetail, String> colLastUpdate;

    @FXML
    private TableColumn<CardDetail, String> colFacilityLastUpdated;

    @FXML
    private TableView<CardDetail> GridCardSummary;

    @FXML
    private TableView<HIVTest> GridClientLastENcounter;

    @FXML
    private TableColumn<HIVTest, String> colTestDate;

    @FXML
    private TableColumn<HIVTest, String> colResult;

    @FXML
    private TableColumn<HIVTest, String> colType;

    @FXML
    private TableColumn<HIVTest, String> colFacility;

    @FXML
    private TableColumn<HIVTest, String> colStrategy;

    @FXML
    private TableView<Identifier> GridClientIdentifiers;

    @FXML
    private TableColumn<Identifier, String> colIdentifierId;

    @FXML
    private TableColumn<Identifier, String> colIdentifierType;

    @FXML
    private TableColumn<Identifier, String> colAssigningAuthority;

    @FXML
    private TableColumn<Identifier, String> colAssigningFacility;

    @FXML
    private TableView<EligiblePerson> GridEligibleList;

    @FXML
    private TableColumn<EligiblePerson, String> colPatientId;

    @FXML
    private TableColumn<EligiblePerson, String> colFirstName;

    @FXML
    private TableColumn<EligiblePerson, String> colMiddleName;

    @FXML
    private TableColumn<EligiblePerson, String> colLastName;

    @FXML
    private TableColumn<EligiblePerson, String> colGender;

    @FXML
    private TableColumn<EligiblePerson, String> colAge;

    @FXML
    private Label lblCardStatus;

    @FXML
    private Button btnInitialiseReader;

    @FXML
    private Button btnConnectReader;

    @FXML
    private Button btnLoadFromEMR;

    @FXML
    private Button btnReadCard;

    @FXML
    private Button btnLoadEligibleList;

    @FXML
    private Label lblUserId;

    @FXML
    private Button btnPushToEMR;

    @FXML
    private JFXComboBox<String> cboDeviceReaderList;

    private SHR shr;

    private EncryptedSHR encryptedSHR;

    //Load Card Details
    private final void loadCardDetails(){
        colCardStatus.setCellValueFactory(new PropertyValueFactory<CardDetail, String>("status"));
        colFacilityLastUpdated.setCellValueFactory(new PropertyValueFactory<CardDetail, String>("lastUpdatedFacility"));
        colLastUpdate.setCellValueFactory(new PropertyValueFactory<CardDetail, String>("lastUpdated"));
        colReason.setCellValueFactory(new PropertyValueFactory<CardDetail, String>("reason"));
        GridCardSummary.setItems(FXCollections.observableArrayList(getCardDetails()));
    }

    private final ObservableList<CardDetail> getCardDetails(){
        CardDetail cardDetail = new CardDetail(
                shr.cARD_DETAILS.sTATUS,
                shr.cARD_DETAILS.rEASON,
                shr.cARD_DETAILS.lAST_UPDATED,
                shr.cARD_DETAILS.lAST_UPDATED_FACILITY
        );
        ObservableList<CardDetail> cardsDetails = FXCollections.observableArrayList(cardDetail);
        return cardsDetails;
    }

    //Load Identifiers
    private final void loadIdentifiers(){
        colIdentifierId.setCellValueFactory(new PropertyValueFactory<Identifier, String>("identifier"));
        colIdentifierType.setCellValueFactory(new PropertyValueFactory<Identifier, String>("identifierType"));
        colAssigningAuthority.setCellValueFactory(new PropertyValueFactory<Identifier, String>("assigningAuthority"));
        colAssigningFacility.setCellValueFactory(new PropertyValueFactory<Identifier, String>("assigningFacility"));
        GridClientIdentifiers.setItems(FXCollections.observableArrayList(getIdentifiers()));
    }

    //Load Identifiers
    private final void loadElligibleList(){
        colPatientId.setCellValueFactory(new PropertyValueFactory<EligiblePerson, String>("patientId"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<EligiblePerson, String>("firstName"));
        colMiddleName.setCellValueFactory(new PropertyValueFactory<EligiblePerson, String>("middleName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<EligiblePerson, String>("lastName"));
        colGender.setCellValueFactory(new PropertyValueFactory<EligiblePerson, String>("gender"));
        colAge.setCellValueFactory(new PropertyValueFactory<EligiblePerson, String>("age"));
        GridEligibleList.setItems(FXCollections.observableArrayList(getEligibilityList()));

		GridEligibleList.setRowFactory( tv -> {
			TableRow<EligiblePerson> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
					EligiblePerson rowData = row.getItem();
					System.out.println(rowData.getFirstName());

					Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
					alert.setTitle("Confirmation Dialog");
					alert.setHeaderText("Card Assignment");
					alert.setContentText("Are you sure you want to assign the current card to "+ rowData + "?");

					ButtonType buttonTypeYes = new ButtonType("Yes");
					ButtonType buttonTypeNo = new ButtonType("No");

					alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

					Optional<ButtonType> result = alert.showAndWait();
					if (result.get() == buttonTypeYes){
					    String url = getURL("HTTP POST - Push the card assignment details to EMR");
					    CardAssignment cardAssignment = new CardAssignment(rowData.getPatientId(), "card-serial-number");
					    String cardAssignmentStr = SHRUtils.getJSON(cardAssignment);
                        String response = APIClient.postData(url, cardAssignmentStr);
                        shr = SHRUtils.getSHR(response);
                        loadCardDetails();
						loadIdentifiers();
						loadHIVTests();
						btnWriteToCard.setDisable(false);
						tpMainTabPane.getSelectionModel().select(0);
                        SmartCardUtils.displayOut(txtProcessLogger, "\n"+ response +"\n");
					} else if (result.get() == buttonTypeNo) {

					}
				}
			});
			return row ;
		});
    }

    //private displayAlert

    private final ObservableList<Identifier> getIdentifiers(){
        ObservableList<Identifier> identifiers = FXCollections.observableArrayList();
        for(int i=0; i < shr.pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID.length;i++ ){
            Identifier identifier = new Identifier(
                    shr.pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID[i].iD,
                    shr.pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID[i].iDENTIFIER_TYPE,
                    shr.pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID[i].aSSIGNING_AUTHORITY,
                    shr.pATIENT_IDENTIFICATION.iNTERNAL_PATIENT_ID[i].aSSIGNING_FACILITY
            );
            identifiers.add(identifier);
        }
        return identifiers;
    }

    private final ObservableList<EligiblePerson> getEligibilityList(){
        ObservableList<EligiblePerson> eligiblePersons = FXCollections.observableArrayList();
        String eligibleListUrl = getURL("HTTP GET - Fetch eligible list from EMR");
        String eligibleListStr = APIClient.fetchData(eligibleListUrl);
        List<EligibleList> eligibleList = SHRUtils.getEligibleList(eligibleListStr);
        for(int i=0; i < eligibleList.size(); i++) {
            EligiblePerson eligiblePerson = new EligiblePerson(
                    eligibleList.get(i).getPatientId(),
                    eligibleList.get(i).getFirstName(),
                    eligibleList.get(i).getMiddleName(),
                    eligibleList.get(i).getLastName(),
                    eligibleList.get(i).getGender(),
                    eligibleList.get(i).getAge()
            );
            eligiblePersons.add(eligiblePerson);
        }
        return eligiblePersons;
    }

    //Load Identifiers
    private final void loadHIVTests(){
        colTestDate.setCellValueFactory(new PropertyValueFactory<HIVTest, String>("testDate"));
        colResult.setCellValueFactory(new PropertyValueFactory<HIVTest, String>("result"));
        colFacility.setCellValueFactory(new PropertyValueFactory<HIVTest, String>("facility"));
        colStrategy.setCellValueFactory(new PropertyValueFactory<HIVTest, String>("strategy"));
        colType.setCellValueFactory(new PropertyValueFactory<HIVTest, String>("type"));
        GridClientLastENcounter.setItems(FXCollections.observableArrayList(getHIVTests()));
    }

    private final ObservableList<HIVTest> getHIVTests(){
        ObservableList<HIVTest> hivTests = FXCollections.observableArrayList();
        for(int i=0; i < shr.hIV_TEST.length;i++ ){
            HIVTest hivTest = new HIVTest(
                    shr.hIV_TEST[i].dATE,
                    shr.hIV_TEST[i].rESULT,
                    shr.hIV_TEST[i].tYPE,
                    shr.hIV_TEST[i].fACILITY,
                    shr.hIV_TEST[i].sTRATEGY
            );
            hivTests.add(hivTest);
        }
        return hivTests;
    }

    @FXML
    void initialize() {
        btnWriteToCard.setDisable(true);
        btnReadCard.setDisable(true);
        readerWriter = new MainSmartCardReadWrite(txtProcessLogger, cboDeviceReaderList);
        btnConnectReader.setDisable(true);
        btnLoadEligibleList.setDisable(true);
    }

    @FXML
    public void initialiseCardReader(ActionEvent event) {
        try {
            MainSmartCardReadWrite reader=new MainSmartCardReadWrite(txtProcessLogger, cboDeviceReaderList);
            readerWriter.initializeReader(btnConnectReader);
            btnConnectReader.setDisable(false);

        } catch(Exception e){
            btnConnectReader.setDisable(true);
           SmartCardUtils.displayOut(txtProcessLogger, "Reader initialization error");
        }
    }

    @FXML
    public void connectReader(ActionEvent event){
            readerWriter.connectReader(btnConnectReader);
            btnReadCard.setDisable(false);
            btnLoadEligibleList.setDisable(false);
    }

    private String getURL(String purpose) {
        String url = "";
        for (Endpoint endpoint : getEndPoints()) {
            if(endpoint.getEndpointPurpose().equals(purpose)){
                if(!(endpoint.getEndpointUrl().isEmpty() || endpoint.getEndpointUrl() == null)) {
                    url = endpoint.getEndpointUrl();
                }
            }
        }
        return url;
    }

    @FXML
    void sendDataToEmr(ActionEvent event) {
        String purpose = "HTTP POST - Push SHR to EMR";
        String url = getURL(purpose);
        if(!url.isEmpty()){
            String shrStr = SHRUtils.getJSON(shr);
            System.out.println(shrStr);
            String response = APIClient.postData(url, shrStr);
            SmartCardUtils.displayOut(txtProcessLogger, "\nResponse from EMR\n "+response);
            btnLoadFromEMR.setDisable(true);
        } else {
            SmartCardUtils.displayOut(txtProcessLogger, "\nPlease specify the `"+purpose+"` endpoint url!");
        }
    }

    @FXML
    void getEligibleList(ActionEvent actionEvent) {
        tpMainTabPane.getSelectionModel().select(3);
        loadElligibleList();
        btnLoadEligibleList.setDisable(true);
        btnReadCard.setDisable(true);
        btnPushToEMR.setDisable(true);
    }

    public List<Endpoint> getEndPoints() {
        Endpoint endpoint;
        List<Endpoint> endpoints = new ArrayList<Endpoint>();
        try {
            Connection dbConn = DBConnection.connect();
            String sql = "Select * from endpoints WHERE void=0";
            ResultSet rs = dbConn.createStatement().executeQuery(sql);
            while(rs.next()){
                endpoint = new Endpoint();
                endpoint.setEndpointPurpose(rs.getString("endpointPurpose"));
                endpoint.setEndpointUrl(rs.getString("endpointUrl"));
                endpoint.setEndpointUsername(rs.getString("username"));
                endpoint.setEndpointPassword(rs.getString("password"));
                endpoints.add(endpoint);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return endpoints;
    }


    public void writeToCard(ActionEvent event) throws ParseException {
        SmartCardUtils.displayOut(txtProcessLogger, "\nWrite to card initiated. ");
        SmartCardUtils.displayOut(txtProcessLogger, "\nFormatting the card... ");
        readerWriter.formatCard();
        MainSmartCardReadWrite writer = new MainSmartCardReadWrite(txtProcessLogger, cboDeviceReaderList);
        String shrStr = SHRUtils.getJSON(shr);
        String encryptedSHR = Encryption.encrypt(shrStr);

        try {
            byte[] compressedMessage = Compression.Compress(encryptedSHR);
            //write the compressed encrypted SHR to card
            //writer.writeCard(SmartCardUtils.getUserFile(SmartCardUtils.PATIENT_CARD_DETAILS_USER_FILE_NAME), compressedMessage);
            shr = null;
			btnWriteToCard.setDisable(true);
			btnReadCard.setDisable(true);
			readerWriter = new MainSmartCardReadWrite(txtProcessLogger, cboDeviceReaderList);
			btnConnectReader.setDisable(true);
			btnLoadEligibleList.setDisable(true);
        }
        catch (IOException e) {
            e.printStackTrace();
            e.getMessage();
        }
    }

    public void LoadEndpointConfig(ActionEvent event) throws ParseException{

        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(
                    Main.class.getResource("ApiEndpoints.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("EndPoint Configuration");
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            // stage.initOwner(
            //       ((Node)event.getSource()).getScene().getWindow() );
            stage.show();

        }catch (Exception e){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation Dialog");
            alert.setHeaderText("P-Smart Middleware");
            alert.setContentText(e.getMessage());

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                // ... user chose OK e.

            } else {
                // ... user chose CANCEL or closed the dialog
            }
        }
    }

    /**
     * should ensure card reader is initialized and connected
     *
     */
    public void readCardContent(ActionEvent event) throws ParseException {
        shr = readerWriter.readCard(SmartCardUtils.getUserFile(SmartCardUtils.PATIENT_DEMOGRAPHICS_USER_FILE_NAME));
        loadCardDetails();
        loadIdentifiers();
        loadHIVTests();
        btnPushToEMR.setDisable(false);
        btnReadCard.setDisable(true);
        btnLoadFromEMR.setDisable(false);
    }

    public void getFromEMR(ActionEvent actionEvent){

        String purpose = "HTTP GET - Fetch SHR from EMR. Takes Card serial as parameter";
        String url = getURL(purpose);

        if(!url.isEmpty()){
            String cardSerialNo = SHRUtils.getCardSerialNo(shr);
            url += (url.endsWith("/")) ? cardSerialNo : "/" + cardSerialNo;
            String SHRStr = APIClient.fetchData(url);
            shr = SHRUtils.getSHR(SHRStr);
            loadCardDetails();
            loadIdentifiers();
            loadHIVTests();
            btnWriteToCard.setDisable(false);
            btnLoadFromEMR.setDisable(true);
            btnPushToEMR.setDisable(true);
            SmartCardUtils.displayOut(txtProcessLogger, "\nSuccessfully retrieved SHR from the EMR\n");
        } else {
            SmartCardUtils.displayOut(txtProcessLogger, "\nPlease specify the `"+purpose+"` endpoint url!");
        }
    }
}
