package com.ondam.notification.service;

import com.ondam.user.entity.PushSubscription;
import com.ondam.user.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Value("${webpush.public-key}")
    private String publicKey;

    @Value("${webpush.private-key}")
    private String privateKey;

    @Value("${webpush.subject}")
    private String subject;

    public void sendPush(
            Long userId,
            String title,
            String content
    ) {

        List<PushSubscription> subscriptions =
                pushSubscriptionRepository.findAllByUserId(userId);

        if (subscriptions.isEmpty()) {
            return;
        }

        try {
            Security.addProvider(new BouncyCastleProvider());

            PushService pushService = new PushService(
                    publicKey,
                    privateKey,
                    subject
            );

            String payload =
                    """
                    {
                      "title": "%s",
                      "content": "%s"
                    }
                    """.formatted(title, content);

            for (PushSubscription subscription : subscriptions) {

                Notification notification =
                        new Notification(
                                subscription.getEndpoint(),
                                subscription.getP256dh(),
                                subscription.getAuth(),
                                payload.getBytes(StandardCharsets.UTF_8)
                        );

                pushService.send(notification);
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Web Push 전송 중 오류가 발생했습니다.",
                    e
            );
        }
    }
}