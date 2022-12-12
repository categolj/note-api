package am.ik.note.sendgrid;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SendGridSender {
	private final Logger log = LoggerFactory.getLogger(SendGridSender.class);

	private final SendGrid sendGrid;

	public SendGridSender(SendGrid sendGrid) {
		this.sendGrid = sendGrid;
	}

	public void sendMail(String to, String subject, String content) {
		try {
			Mail mail = new Mail(new Email("noreply@ik.am"), subject, new Email(to),
					new Content("text/plain", content));
			mail.setReplyTo(new Email("makingx+hajiboot3@gmail.com"));

			Request request = new Request();
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			Response response = this.sendGrid.api(request);
			if (response.getStatusCode() != 202) {
				log.warn("statusCode = {}", response.getStatusCode());
				log.warn("headers = {}", response.getHeaders());
				log.warn("body = {}", response.getBody());
				throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
						"Sending a mail failed ... Please register again later.");
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}