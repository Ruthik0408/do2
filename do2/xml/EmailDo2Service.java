package itsdc.dolphin.do2.xml;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.sun.mail.smtp.SMTPAddressFailedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import itsdc.dolphin.models.Misc;
import itsdc.dolphin.repositories.MiscRepository;

@Service
public class EmailDo2Service {
	
	@Autowired
	MiscRepository miscRepo;

    @Async("emailExecutor")
    public void sendEmailAsync(String zipFilePath) {
       	try {

			String senderMail = null;
			String receiverMail1 = null;
			String receiverMail2 = null;
			String password = null;

			List<Misc> sender = miscRepo.findAllByKey1AndCurrent("Nic_Email_Id", true);

			if (sender != null && !sender.isEmpty()) {

				senderMail = sender.get(0).getValue1().trim();
			}

			List<Misc> passwd = miscRepo.findAllByKey1AndCurrent("Nic_Email_Passwd", true);

			if (passwd != null && !passwd.isEmpty()) {

				password = passwd.get(0).getValue1().trim();
			}

			List<Misc> recipient1 = miscRepo.findAllByKey1AndCurrent("IHQ_Email_Id", true);
			if (recipient1 != null && !recipient1.isEmpty()) {
				receiverMail1 = recipient1.get(0).getValue1().trim();
			}

			List<Misc> recipient2 = miscRepo.findAllByKey1AndCurrent("RecordOffice_Email_Id", true);
			if (recipient2 != null && !recipient2.isEmpty()) {
				receiverMail2 = recipient2.get(0).getValue1().trim();
			}

			if (senderMail == null || receiverMail1 == null || password == null) {

				System.out.println("Email configuration incomplete");

				return;
			}

			String protocol = "164.100.13.60";

			String subject = "Brahma DO2 Rejection Details";

			String body = "Please find attached ZIP containing " + "rejected DO2 acknowledgement XML files.";

			System.out.println("Sending Email With Attachment : " + zipFilePath);

			String receiverMail = receiverMail1;

			if (receiverMail2 != null && !receiverMail2.isEmpty()) {
				receiverMail += "," + receiverMail2;
			}

			String msg = sendMailWithAttachment(receiverMail, subject, body, senderMail, password, protocol,
					zipFilePath);

			System.out.println("Email Status : " + msg);

		} catch (Exception e) {

			e.printStackTrace();
		}
    }

    public String sendMailWithAttachment(String mailId, String subject, String body,
            String senderMailId, String senderMailPwd, String protocol, String filePath) {
        Properties props = new Properties();
	    props.put("mail.smtp.host", "164.100.13.60");
	    props.put("mail.smtp.auth", "false");
	    props.put("mail.smtp.port", "25");
	    props.put("mail.smtp.connectiontimeout", "5000");
	    props.put("mail.smtp.timeout", "5000");
	    props.put("mail.smtp.writetimeout", "5000");

	    if (protocol != null && !protocol.isEmpty()) {
	        props.put("mail.transport.protocol", protocol);
	    }

	    Session session = Session.getInstance(props);

	    System.out.println(
	            "------------params for email--------:" + mailId + "----" + senderMailId + "----" + senderMailPwd);

	    int maxAttempts = 3;
	    long backoffMillis = 2000; // start at 2s, doubles each retry

	    for (int attempt = 1; attempt <= maxAttempts; attempt++) {

	        try {

	            InternetAddress fromAddress = null;
	            try {
	                fromAddress = new InternetAddress(senderMailId, "Dolphin");
	            } catch (UnsupportedEncodingException e) {
	                fromAddress = new InternetAddress(senderMailId);
	            }

	            MimeMessage message = new MimeMessage(session);
	            message.setFrom(fromAddress);
	            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailId));
	            message.setSubject(subject);

	            MimeBodyPart bodyPart = new MimeBodyPart();
	            bodyPart.setContent(body, "text/html; charset=utf-8");

	            Multipart multipart = new MimeMultipart();
	            multipart.addBodyPart(bodyPart);

	            if (filePath != null) {
	                File file = new File(filePath);
	                if (file.exists()) {
	                    MimeBodyPart attachmentPart = new MimeBodyPart();
	                    attachmentPart.attachFile(file);
	                    multipart.addBodyPart(attachmentPart);
	                    System.out.println("Attached File : " + file.getAbsolutePath());
	                }
	            }

	            message.setContent(multipart);
	            Transport.send(message);

	            System.out.println("EMail Sent Successfully!! (attempt " + attempt + ")");
	            return "Mail Sent to Email-Id";

	        } catch (SMTPAddressFailedException e) {
	            // Bad address won't fix itself on retry - fail immediately
	            System.out.println("------------------landed here---------------............................");
	            return "BadAddress";

	        } catch (SendFailedException | AddressException e) {
	            // Bad address won't fix itself on retry - fail immediately
	            System.out.println("-------------------I am here nooooooooooooo..................");
	            return "BadAddress";

	        } catch (MessagingException e) {
	            // Covers MailConnectException, timeouts, and other transient SMTP issues - worth retrying
	            System.out.println("Email attempt " + attempt + " of " + maxAttempts + " failed: " + e.getMessage());

	            if (attempt == maxAttempts) {
	                e.printStackTrace();
	                return "failure";
	            }

	            try {
	                Thread.sleep(backoffMillis);
	            } catch (InterruptedException ie) {
	                Thread.currentThread().interrupt();
	                return "failure";
	            }

	            backoffMillis *= 2; // 2s, 4s, 8s

	        } catch (Exception e) {
	            // Unexpected error - not necessarily transient, don't retry
	            e.printStackTrace();
	            return "failure";
	        }
	    }

	    return "failure";
    }
}
