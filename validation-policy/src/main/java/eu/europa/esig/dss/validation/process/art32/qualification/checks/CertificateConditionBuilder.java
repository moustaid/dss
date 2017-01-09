package eu.europa.esig.dss.validation.process.art32.qualification.checks;

import eu.europa.esig.dss.validation.process.art32.EIDASConstants;
import eu.europa.esig.dss.validation.process.art32.qualification.checks.qualified.PostEIDASCertificateQualified;
import eu.europa.esig.dss.validation.process.art32.qualification.checks.qualified.PreEIDASCertificateQualified;
import eu.europa.esig.dss.validation.process.art32.qualification.checks.sscd.PostEIDASCertificateSSCD;
import eu.europa.esig.dss.validation.process.art32.qualification.checks.sscd.PreEIDASCertificateSSCD;
import eu.europa.esig.dss.validation.reports.wrapper.CertificateWrapper;

public class CertificateConditionBuilder {

	public static CertificateCondition certificateQualified(CertificateWrapper certificate) {
		if (EIDASConstants.EIDAS_DATE.before(certificate.getNotBefore())) {
			return new PreEIDASCertificateQualified();
		} else {
			return new PostEIDASCertificateQualified();
		}
	}

	public static CertificateCondition certificateSSCD(CertificateWrapper certificate) {
		if (EIDASConstants.EIDAS_DATE.before(certificate.getNotBefore())) {
			return new PreEIDASCertificateSSCD();
		} else {
			return new PostEIDASCertificateSSCD();
		}
	}

}
