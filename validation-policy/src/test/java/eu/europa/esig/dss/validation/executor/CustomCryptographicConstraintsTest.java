package eu.europa.esig.dss.validation.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.jaxb.detailedreport.XmlBasicBuildingBlocks;
import eu.europa.esig.dss.jaxb.detailedreport.XmlName;
import eu.europa.esig.dss.jaxb.diagnostic.DiagnosticData;
import eu.europa.esig.dss.validation.policy.Context;
import eu.europa.esig.dss.validation.policy.EtsiValidationPolicy;
import eu.europa.esig.dss.validation.policy.XmlUtils;
import eu.europa.esig.dss.validation.policy.rules.Indication;
import eu.europa.esig.dss.validation.process.MessageTag;
import eu.europa.esig.dss.validation.reports.DetailedReport;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.validation.reports.SimpleReport;
import eu.europa.esig.jaxb.policy.Algo;
import eu.europa.esig.jaxb.policy.AlgoExpirationDate;
import eu.europa.esig.jaxb.policy.BasicSignatureConstraints;
import eu.europa.esig.jaxb.policy.ConstraintsParameters;
import eu.europa.esig.jaxb.policy.CryptographicConstraint;
import eu.europa.esig.jaxb.policy.Level;
import eu.europa.esig.jaxb.policy.ListAlgo;
import eu.europa.esig.jaxb.policy.RevocationConstraints;
import eu.europa.esig.jaxb.policy.SignatureConstraints;
import eu.europa.esig.jaxb.policy.TimestampConstraints;
import junit.framework.Assert;

public class CustomCryptographicConstraintsTest extends AbstractValidationExecutorTest {
	
	private ConstraintsParameters constraintsParameters = null;
	private CustomProcessExecutor executor = null;
	private EtsiValidationPolicy validationPolicy = null;

	private static final String ALGORITHM_DSA = "DSA";
	private static final String ALGORITHM_RSA = "RSA";
	private static final String ALGORITHM_RSA2048 = "RSA2048";
	private static final String ALGORITHM_RSA4096 = "RSA4096";
	private static final String ALGORITHM_SHA1 = "SHA1";
	private static final String ALGORITHM_SHA256 = "SHA256";
	
	private static final String BIT_SIZE_4096 = "4096";
	
	private String validationPolicyFile = null; 

	/**
	 * Test for signature using SHA256 as the Digest algorithm and RSA 2048 as the Encryption Algorithm
	 * Validation date is set on 2018-02-06T09:39:33
	 */
	@Test
	public void defaultOnlyCryptographicConstrantTest() throws Exception {
		
		initializeExecutor("src/test/resources/universign.xml");
		validationPolicyFile = "src/test/resources/policy/default-only-constraint-policy.xml";
		
		Indication result = null;
		DetailedReport detailedReport = null;
		XmlBasicBuildingBlocks revocationBasicBuildingBlock = null;
		
		result = defaultConstraintValidationDateIsBeforeExpirationDateTest(ALGORITHM_SHA256);
		assertEquals(Indication.TOTAL_PASSED, result);
		checkErrorMessageAbsence(MessageTag.ASCCM_ANS_5);
		
		result = defaultConstraintAlgorithmExpiredTest(ALGORITHM_SHA256);
		assertEquals(Indication.INDETERMINATE, result);
		
		result = defaultConstraintSetLevelForPreviousValidationPolicy(Level.WARN);
		assertEquals(Indication.TOTAL_PASSED, result);
		checkErrorMessagePresence(MessageTag.ASCCM_ANS_5);
		
		result = defaultConstraintAlgorithmExpiredTest(ALGORITHM_SHA1); // some other algorithm is expired
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = defaultConstraintAlgorithmExpirationDateIsNotDefined(ALGORITHM_RSA2048);
		assertEquals(Indication.INDETERMINATE, result);

		result = defaultConstraintSetLevelForPreviousValidationPolicy(Level.WARN);
		assertEquals(Indication.TOTAL_PASSED, result);
		checkErrorMessagePresence(MessageTag.ASCCM_ANS_4);
		
		result = defaultConstraintAlgorithmExpirationDateIsNotDefined(ALGORITHM_RSA4096); // some other algorithm is expired
		assertEquals(Indication.TOTAL_PASSED, result);
		checkErrorMessageAbsence(MessageTag.ASCCM_ANS_4);
		
		result = defaultConstraintAcceptableDigestAlgorithmIsNotDefined(ALGORITHM_SHA256);
		assertEquals(Indication.INDETERMINATE, result);
		detailedReport = createDetailedReport();
		revocationBasicBuildingBlock = detailedReport.getBasicBuildingBlockById(detailedReport.getRevocationIds().get(0));
		assertEquals(Indication.INDETERMINATE, revocationBasicBuildingBlock.getSAV().getConclusion().getIndication());
		assertEquals(Indication.INDETERMINATE, detailedReport.getTimestampValidationIndication(detailedReport.getTimestampIds().get(0)));
		checkRevocationErrorPresence(detailedReport, MessageTag.ASCCM_ANS_2, true);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_2, true);

		result = defaultConstraintSetLevelForPreviousValidationPolicy(Level.WARN);
		assertEquals(Indication.TOTAL_PASSED, result);
		checkErrorMessagePresence(MessageTag.ASCCM_ANS_2);
		
		result = defaultConstraintAcceptableDigestAlgorithmIsNotDefined(ALGORITHM_SHA1); // some other algorithm is not defined
		assertEquals(Indication.TOTAL_PASSED, result);
		detailedReport = createDetailedReport();
		checkErrorMessageAbsence(detailedReport, MessageTag.ASCCM_ANS_2);
		revocationBasicBuildingBlock = detailedReport.getBasicBuildingBlockById(detailedReport.getRevocationIds().get(0));
		assertEquals(Indication.PASSED, revocationBasicBuildingBlock.getSAV().getConclusion().getIndication());
		assertEquals(Indication.PASSED, detailedReport.getTimestampValidationIndication(detailedReport.getTimestampIds().get(0)));
		checkRevocationErrorPresence(detailedReport, MessageTag.ASCCM_ANS_2, false);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_2, false);
		
		result = defaultConstraintAcceptableEncryptionAlgorithmIsNotDefined(ALGORITHM_RSA);
		assertEquals(Indication.INDETERMINATE, result);

		result = defaultConstraintSetLevelForPreviousValidationPolicy(Level.WARN);
		assertEquals(Indication.TOTAL_PASSED, result);
		checkErrorMessagePresence(MessageTag.ASCCM_ANS_1);
		
		result = defaultConstraintAcceptableEncryptionAlgorithmIsNotDefined(ALGORITHM_DSA); // some other algorithm is not defined
		assertEquals(Indication.TOTAL_PASSED, result);
		checkErrorMessageAbsence(MessageTag.ASCCM_ANS_1);
		
		result = defaultConstraintLargeMiniPublicKeySize(ALGORITHM_RSA);
		assertEquals(Indication.INDETERMINATE, result);

		result = defaultConstraintSetLevelForPreviousValidationPolicy(Level.WARN);
		assertEquals(Indication.TOTAL_PASSED, result);
		checkErrorMessagePresence(MessageTag.ASCCM_ANS_3);
		
		result = defaultConstraintLargeMiniPublicKeySize(ALGORITHM_DSA); // some other algorithm is changed
		assertEquals(Indication.TOTAL_PASSED, result);
		checkErrorMessageAbsence(MessageTag.ASCCM_ANS_3);
		
	}

	@Test
	public void overrideDefaultCryptographicConstrantTest() throws Exception {
		
		initializeExecutor("src/test/resources/universign.xml");
		validationPolicyFile = "src/test/resources/policy/all-constraint-specified-policy.xml";
		
		Indication result = null;
		DetailedReport detailedReport = null;
		
		// tests change only default constraints
		result = defaultConstraintValidationDateIsBeforeExpirationDateTest(ALGORITHM_SHA256);
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = defaultConstraintAlgorithmExpiredTest(ALGORITHM_SHA256);
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = defaultConstraintAlgorithmExpirationDateIsNotDefined(ALGORITHM_SHA256);
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = defaultConstraintAlgorithmExpirationDateIsNotDefined(ALGORITHM_RSA2048);
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = defaultConstraintAcceptableDigestAlgorithmIsNotDefined(ALGORITHM_SHA256);
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = defaultConstraintAcceptableEncryptionAlgorithmIsNotDefined(ALGORITHM_RSA);
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = defaultConstraintLargeMiniPublicKeySize(ALGORITHM_RSA);
		assertEquals(Indication.TOTAL_PASSED, result);
		
		// tests change main Signature constraints
		result = signatureConstraintAlgorithmExpired(ALGORITHM_SHA256, "2015-01-01");
		assertEquals(Indication.INDETERMINATE, result);
		
		result = signatureConstraintAlgorithmExpired(ALGORITHM_SHA1, "2015-01-01"); // some other algorithm is changed
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = signatureConstraintAlgorithmExpirationDateIsNotDefined(ALGORITHM_SHA256);
		assertEquals(Indication.INDETERMINATE, result);
		
		result = signatureConstraintAlgorithmExpirationDateIsNotDefined(ALGORITHM_SHA1); // some other algorithm is changed
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = signatureConstraintAlgorithmExpirationDateIsNotDefined(ALGORITHM_RSA2048);
		assertEquals(Indication.INDETERMINATE, result);
		
		result = signatureConstraintAlgorithmExpirationDateIsNotDefined(ALGORITHM_RSA4096); // some other algorithm is changed
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = signatureConstraintAcceptableDigestAlgorithmIsNotDefined(ALGORITHM_SHA256);
		assertEquals(Indication.INDETERMINATE, result);
		
		result = signatureConstraintAcceptableDigestAlgorithmIsNotDefined(ALGORITHM_SHA1); // some other algorithm is changed
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = signatureConstraintAcceptableEncriptionAlgorithmIsNotDefined(ALGORITHM_RSA);
		assertEquals(Indication.INDETERMINATE, result);
		
		result = signatureConstraintAcceptableEncriptionAlgorithmIsNotDefined(ALGORITHM_DSA); // some other algorithm is changed
		assertEquals(Indication.TOTAL_PASSED, result);
		
		result = signatureConstraintLargeMiniPublicKeySize(ALGORITHM_RSA);
		assertEquals(Indication.INDETERMINATE, result);
		
		result = signatureConstraintLargeMiniPublicKeySize(ALGORITHM_DSA); // some other algorithm is changed
		assertEquals(Indication.TOTAL_PASSED, result);
		
		detailedReport = createDetailedReport();
		XmlBasicBuildingBlocks revocationBasicBuildingBlock = detailedReport.getBasicBuildingBlockById(detailedReport.getRevocationIds().get(0));
		assertEquals(Indication.PASSED, revocationBasicBuildingBlock.getSAV().getConclusion().getIndication());
		checkErrorMessageAbsence(detailedReport, MessageTag.ASCCM_ANS_2);
		
		// Revocation data tests
		result = revocationConstraintAcceptableDigestAlgorithmIsNotDefined(ALGORITHM_SHA256);
		detailedReport = createDetailedReport();
		revocationBasicBuildingBlock = detailedReport.getBasicBuildingBlockById(detailedReport.getRevocationIds().get(0));
		assertEquals(Indication.INDETERMINATE, revocationBasicBuildingBlock.getSAV().getConclusion().getIndication());
		assertEquals(Indication.PASSED, detailedReport.getTimestampValidationIndication(detailedReport.getTimestampIds().get(0)));
		checkRevocationErrorPresence(detailedReport, MessageTag.ASCCM_ANS_2, true);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_2, false);
		
		// Timestamp tests
		result = timestampConstraintAcceptableDigestAlgorithmIsNotDefined(ALGORITHM_SHA256);
		detailedReport = createDetailedReport();
		revocationBasicBuildingBlock = detailedReport.getBasicBuildingBlockById(detailedReport.getRevocationIds().get(0));
		assertEquals(Indication.PASSED, revocationBasicBuildingBlock.getSAV().getConclusion().getIndication());
		assertEquals(Indication.INDETERMINATE, detailedReport.getTimestampValidationIndication(detailedReport.getTimestampIds().get(0)));
		checkRevocationErrorPresence(detailedReport, MessageTag.ASCCM_ANS_2, false);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_2, true);
		
	}

	@Test
	public void pastSignatureValidationTest() throws Exception {
		
		initializeExecutor("src/test/resources/diag_data_pastSigValidation.xml");
		validationPolicyFile = "src/test/resources/policy/all-constraint-specified-policy.xml";
		
		Indication result = null;
		DetailedReport detailedReport = null;

		result = signatureConstraintAlgorithmExpired(ALGORITHM_SHA256, "2020-01-01");
		assertEquals(Indication.TOTAL_PASSED, result);
		detailedReport = createDetailedReport();
		checkBasicSignatureErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, false);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, false);
		
		result = signatureConstraintAlgorithmExpired(ALGORITHM_SHA256, "2019-01-01");
		assertEquals(Indication.TOTAL_PASSED, result);
		detailedReport = createDetailedReport();
		checkBasicSignatureErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, true);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, false);
		
		result = signatureConstraintAlgorithmExpired(ALGORITHM_SHA256, "2018-01-01");
		assertEquals(Indication.INDETERMINATE, result);
		detailedReport = createDetailedReport();
		checkBasicSignatureErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, true);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, false);
		
		result = signatureConstraintAlgorithmExpired(ALGORITHM_SHA1, "2018-01-01");
		assertEquals(Indication.TOTAL_PASSED, result);
		detailedReport = createDetailedReport();
		checkBasicSignatureErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, false);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, false);
		
		result = signatureConstraintAlgorithmExpired(ALGORITHM_RSA2048, "2020-01-01");
		assertEquals(Indication.TOTAL_PASSED, result);
		detailedReport = createDetailedReport();
		checkBasicSignatureErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, false);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, false);
		
		result = signatureConstraintAlgorithmExpired(ALGORITHM_RSA2048, "2019-01-01");
		assertEquals(Indication.TOTAL_PASSED, result);
		detailedReport = createDetailedReport();
		checkBasicSignatureErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, true);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, false);
		
		result = signatureConstraintAlgorithmExpired(ALGORITHM_RSA2048, "2018-01-01");
		assertEquals(Indication.INDETERMINATE, result);
		detailedReport = createDetailedReport();
		checkBasicSignatureErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, true);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, false);
		
		result = signatureConstraintAlgorithmExpired(ALGORITHM_RSA2048, "2019-01-01");
		assertEquals(Indication.TOTAL_PASSED, result);
		detailedReport = createDetailedReport();
		checkBasicSignatureErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, true);
		checkTimestampErrorPresence(detailedReport, MessageTag.ASCCM_ANS_5, false);
		
	}
	
	private Indication defaultConstraintValidationDateIsBeforeExpirationDateTest(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint defaultCryptographicConstraint = constraintsParameters.getCryptographic();
		setAlgoExpirationDate(defaultCryptographicConstraint, algorithm, "2020-02-24");
		constraintsParameters.setCryptographic(defaultCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private Indication defaultConstraintAlgorithmExpiredTest(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint defaultCryptographicConstraint = constraintsParameters.getCryptographic();
		setAlgoExpirationDate(defaultCryptographicConstraint, algorithm, "2015-02-24");
		constraintsParameters.setCryptographic(defaultCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private Indication defaultConstraintAlgorithmExpirationDateIsNotDefined(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint defaultCryptographicConstraint = constraintsParameters.getCryptographic();
		AlgoExpirationDate algoExpirationDate = defaultCryptographicConstraint.getAlgoExpirationDate();
		List<Algo> algorithms = algoExpirationDate.getAlgo();
		removeAlgorithm(algorithms, algorithm);
		constraintsParameters.setCryptographic(defaultCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private Indication defaultConstraintAcceptableDigestAlgorithmIsNotDefined(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint defaultCryptographicConstraint = constraintsParameters.getCryptographic();
		ListAlgo listAlgo = defaultCryptographicConstraint.getAcceptableDigestAlgo();
		List<Algo> algorithms = listAlgo.getAlgo();
		removeAlgorithm(algorithms, algorithm);
		constraintsParameters.setCryptographic(defaultCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private Indication defaultConstraintAcceptableEncryptionAlgorithmIsNotDefined(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint defaultCryptographicConstraint = constraintsParameters.getCryptographic();
		ListAlgo listAlgo = defaultCryptographicConstraint.getAcceptableEncryptionAlgo();
		List<Algo> algorithms = listAlgo.getAlgo();
		removeAlgorithm(algorithms, algorithm);
		constraintsParameters.setCryptographic(defaultCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private Indication defaultConstraintLargeMiniPublicKeySize(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint defaultCryptographicConstraint = constraintsParameters.getCryptographic();
		ListAlgo listAlgo = defaultCryptographicConstraint.getMiniPublicKeySize();
		List<Algo> algorithms = listAlgo.getAlgo();
		setAlgorithmSize(algorithms, algorithm, BIT_SIZE_4096);
		constraintsParameters.setCryptographic(defaultCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private Indication defaultConstraintSetLevelForPreviousValidationPolicy(Level level) throws Exception {
		ConstraintsParameters constraintsParameters = this.constraintsParameters;
		CryptographicConstraint defaultCryptographicConstraint = constraintsParameters.getCryptographic();
		defaultCryptographicConstraint.setLevel(level);
		constraintsParameters.setCryptographic(defaultCryptographicConstraint);
		setSignatureCryptographicConstraint(constraintsParameters, new CryptographicConstraint());
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private Indication signatureConstraintAlgorithmExpired(String algorithm, String date) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint sigCryptographicConstraint = getSignatureCryptographicConstraint(constraintsParameters);
		setAlgoExpirationDate(sigCryptographicConstraint, algorithm, date);
		setSignatureCryptographicConstraint(constraintsParameters, sigCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private Indication signatureConstraintAlgorithmExpirationDateIsNotDefined(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint sigCryptographicConstraint = getSignatureCryptographicConstraint(constraintsParameters);
		AlgoExpirationDate algoExpirationDate = sigCryptographicConstraint.getAlgoExpirationDate();
		List<Algo> algorithms = algoExpirationDate.getAlgo();
		removeAlgorithm(algorithms, algorithm);
		setSignatureCryptographicConstraint(constraintsParameters, sigCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private Indication signatureConstraintAcceptableDigestAlgorithmIsNotDefined(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint sigCryptographicConstraint = getSignatureCryptographicConstraint(constraintsParameters);
		ListAlgo listAlgo = sigCryptographicConstraint.getAcceptableDigestAlgo();
		List<Algo> algorithms = listAlgo.getAlgo();
		removeAlgorithm(algorithms, algorithm);
		setSignatureCryptographicConstraint(constraintsParameters, sigCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private Indication signatureConstraintAcceptableEncriptionAlgorithmIsNotDefined(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint sigCryptographicConstraint = getSignatureCryptographicConstraint(constraintsParameters);
		ListAlgo listAlgo = sigCryptographicConstraint.getAcceptableEncryptionAlgo();
		List<Algo> algorithms = listAlgo.getAlgo();
		removeAlgorithm(algorithms, algorithm);
		setSignatureCryptographicConstraint(constraintsParameters, sigCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private Indication signatureConstraintLargeMiniPublicKeySize(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint sigCryptographicConstraint = getSignatureCryptographicConstraint(constraintsParameters);
		ListAlgo listAlgo = sigCryptographicConstraint.getMiniPublicKeySize();
		List<Algo> algorithms = listAlgo.getAlgo();
		setAlgorithmSize(algorithms, algorithm, BIT_SIZE_4096);
		setSignatureCryptographicConstraint(constraintsParameters, sigCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}

	private Indication revocationConstraintAcceptableDigestAlgorithmIsNotDefined(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint revocationCryptographicConstraint = getRevocationCryptographicConstraint(constraintsParameters);
		ListAlgo listAlgo = revocationCryptographicConstraint.getAcceptableDigestAlgo();
		List<Algo> algorithms = listAlgo.getAlgo();
		removeAlgorithm(algorithms, algorithm);
		revocationCryptographicConstraint.setAcceptableDigestAlgo(listAlgo);
		setRevocationCryptographicConstraint(constraintsParameters, revocationCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}

	private Indication timestampConstraintAcceptableDigestAlgorithmIsNotDefined(String algorithm) throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters();
		CryptographicConstraint timestampCryptographicConstraint = getTimestampCryptographicConstraint(constraintsParameters);
		ListAlgo listAlgo = timestampCryptographicConstraint.getAcceptableDigestAlgo();
		List<Algo> algorithms = listAlgo.getAlgo();
		removeAlgorithm(algorithms, algorithm);
		timestampCryptographicConstraint.setAcceptableDigestAlgo(listAlgo);
		setTimestampCryptographicConstraints(constraintsParameters, timestampCryptographicConstraint);
		setValidationPolicy(constraintsParameters);
		SimpleReport simpleReport = createSimpleReport();
		return simpleReport.getIndication(simpleReport.getFirstSignatureId());
	}
	
	private CryptographicConstraint getSignatureCryptographicConstraint(ConstraintsParameters constraintsParameters) {
		SignatureConstraints sigConstraint = constraintsParameters.getSignatureConstraints();
		return sigConstraint.getBasicSignatureConstraints().getCryptographic();
	}
	
	private void setSignatureCryptographicConstraint(ConstraintsParameters constraintsParameters, CryptographicConstraint cryptographicConstraint) {
		SignatureConstraints sigConstraint = constraintsParameters.getSignatureConstraints();
		BasicSignatureConstraints basicSignatureConstraints = sigConstraint.getBasicSignatureConstraints();
		basicSignatureConstraints.setCryptographic(cryptographicConstraint);
		sigConstraint.setBasicSignatureConstraints(basicSignatureConstraints);
		constraintsParameters.setSignatureConstraints(sigConstraint);
	}
	
	private CryptographicConstraint getRevocationCryptographicConstraint(ConstraintsParameters constraintsParameters) {
		RevocationConstraints revocationConstraints = constraintsParameters.getRevocation();
		return revocationConstraints.getBasicSignatureConstraints().getCryptographic();
	}
	
	private RevocationConstraints setRevocationCryptographicConstraint(ConstraintsParameters constraintsParameters, CryptographicConstraint cryptographicConstraint) {
		RevocationConstraints revocationConstraints = constraintsParameters.getRevocation();
		BasicSignatureConstraints basicSignatureConstraints = revocationConstraints.getBasicSignatureConstraints();
		basicSignatureConstraints.setCryptographic(cryptographicConstraint);
		revocationConstraints.setBasicSignatureConstraints(basicSignatureConstraints);
		constraintsParameters.setRevocation(revocationConstraints);
		return revocationConstraints;
	}
	
	private CryptographicConstraint getTimestampCryptographicConstraint(ConstraintsParameters constraintsParameters) {
		TimestampConstraints timestampConstraints = constraintsParameters.getTimestamp();
		return timestampConstraints.getBasicSignatureConstraints().getCryptographic();
	}
	
	private TimestampConstraints setTimestampCryptographicConstraints(ConstraintsParameters constraintsParameters, CryptographicConstraint cryptographicConstraint) {
		TimestampConstraints timestampConstraints = constraintsParameters.getTimestamp();
		BasicSignatureConstraints basicSignatureConstraints = timestampConstraints.getBasicSignatureConstraints();
		basicSignatureConstraints.setCryptographic(cryptographicConstraint);
		timestampConstraints.setBasicSignatureConstraints(basicSignatureConstraints);
		constraintsParameters.setTimestamp(timestampConstraints);
		return timestampConstraints;
	}
	
	private void checkErrorMessageAbsence(MessageTag message) {
		Reports reports = createReports();
		DetailedReport detailedReport = reports.getDetailedReport();
		checkErrorMessageAbsence(detailedReport, message);
	}
	
	private void checkErrorMessageAbsence(DetailedReport detailedReport, MessageTag message) {
		assertTrue(!detailedReport.getWarnings(detailedReport.getFirstSignatureId()).contains(message.getMessage()));
		assertTrue(!detailedReport.getErrors(detailedReport.getFirstSignatureId()).contains(message.getMessage()));
	}
	
	private void checkErrorMessagePresence(MessageTag message) {
		Reports reports = createReports();
		DetailedReport detailedReport = reports.getDetailedReport();
		checkErrorMessagePresence(detailedReport, message);
	}

	private void checkErrorMessagePresence(DetailedReport detailedReport, MessageTag message) {
		assertTrue(detailedReport.getWarnings(detailedReport.getFirstSignatureId()).contains(message.getMessage()));
		assertTrue(detailedReport.getErrors(detailedReport.getFirstSignatureId()).contains(message.getMessage()));
	}
	
	private void checkBasicSignatureErrorPresence(DetailedReport detailedReport, MessageTag message, boolean present) {
		List<XmlName> errors = detailedReport.getBasicBuildingBlockById(detailedReport.getFirstSignatureId()).getConclusion().getErrors();
		assertTrue(!present ^ xmlListContainsMessage(errors, message));
	}
	
	private void checkRevocationErrorPresence(DetailedReport detailedReport, MessageTag message, boolean present) {
		List<XmlName> listErrors = detailedReport.getBasicBuildingBlockById(detailedReport.getRevocationIds().get(0)).getSAV().getConclusion().getErrors();
		assertTrue(!present ^ xmlListContainsMessage(listErrors, message));
	}
	
	private void checkTimestampErrorPresence(DetailedReport detailedReport, MessageTag message, boolean present) {
		List<XmlName> listErrors = detailedReport.getBasicBuildingBlockById(detailedReport.getTimestampIds().get(0)).getSAV().getConclusion().getErrors();
		assertTrue(!present ^ xmlListContainsMessage(listErrors, message));
	}
	
	private boolean xmlListContainsMessage(List<XmlName> list, MessageTag message) {
		for (XmlName name : list) {
			if (message.getMessage().equals(name.getValue())) {
				return true;
			}
		}
		return false;
	}
	
	private void initializeExecutor(String diagnosticDataFile) throws Exception {
		FileInputStream fis = new FileInputStream(diagnosticDataFile);
		DiagnosticData diagnosticData = XmlUtils.getJAXBObjectFromString(fis, DiagnosticData.class, "/xsd/DiagnosticData.xsd");
		assertNotNull(diagnosticData);

		executor = new CustomProcessExecutor();
		executor.setDiagnosticData(diagnosticData);
		executor.setCurrentTime(diagnosticData.getValidationDate());
	}

	private ConstraintsParameters loadConstraintsParameters() throws Exception {
		ConstraintsParameters constraintsParameters = loadConstraintsParameters(validationPolicyFile);
		this.constraintsParameters = constraintsParameters;
		return constraintsParameters;
	}
	
	private void setValidationPolicy(ConstraintsParameters constraintsParameters) {
		validationPolicy = new EtsiValidationPolicy(constraintsParameters);
	}
	
	private Reports createReports() {
		executor.setValidationPolicy(validationPolicy);
		return executor.execute();
	}
	
	private SimpleReport createSimpleReport() {
		Reports reports = createReports();
		return reports.getSimpleReport();
	}
	
	private DetailedReport createDetailedReport() {
		Reports reports = createReports();
		return reports.getDetailedReport();
	}
	
	private void setAlgoExpirationDate(CryptographicConstraint cryptographicConstraint, String algorithmName, String expirationDate) {
		
		AlgoExpirationDate algoExpirationDate = cryptographicConstraint.getAlgoExpirationDate();
		List<Algo> algorithms = algoExpirationDate.getAlgo();
		boolean listContainsAlgorithms = false;
		for (Algo algorithm : algorithms) {
			if (algorithm.getValue().equals(algorithmName)) {
				algorithm.setDate(expirationDate);
				listContainsAlgorithms = true;
			}
		}
		if (!listContainsAlgorithms) {
			Algo algo = new Algo();
			algo.setValue(algorithmName);
			algo.setDate(expirationDate);
			algorithms.add(algo);
		}
		
	}
	
	private void removeAlgorithm(List<Algo> algorithms, String algorithmName) {
		Iterator<Algo> iterator = algorithms.iterator();
		while(iterator.hasNext()) {
			Algo algo = iterator.next();
			if (algo.getValue().equals(algorithmName)) {
				iterator.remove();
			}
		}
	}
	
	private void setAlgorithmSize(List<Algo> algorithms, String algorithm, String size) {
		for (Algo algo : algorithms) {
			if (algo.getValue().equals(algorithm)) {
				algo.setSize(BIT_SIZE_4096);
				return;
			}
		}
	}

}