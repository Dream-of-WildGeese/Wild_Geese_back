package com.ondam.notification.service;

import com.ondam.user.entity.PushSubscription;
import com.ondam.user.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
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

        System.out.println("Push userId = " + userId);
        System.out.println("Push 구독 개수 = " + subscriptions.size());

        if (subscriptions.isEmpty()) {
            System.out.println("Push 구독 정보 없음");
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

                try {

                    System.out.println(
                            "Push endpoint = " +
                                    subscription.getEndpoint()
                    );

                    Notification notification =
                            new Notification(
                                    subscription.getEndpoint(),
                                    subscription.getP256dh(),
                                    subscription.getAuth(),
                                    payload.getBytes(StandardCharsets.UTF_8)
                            );

                    HttpResponse response =
                            pushService.send(notification);

                    int statusCode =
                            response.getStatusLine().getStatusCode();

                    System.out.println(
                            "Push 응답 코드 = " + statusCode
                    );

                    System.out.println(
                            "Push 응답 상태 = " +
                                    response.getStatusLine()
                    );

                    // 만료된 Push 구독
                    if (statusCode == 404 || statusCode == 410) {

                        System.out.println(
                                "만료된 Push 구독 삭제"
                        );

                        pushSubscriptionRepository
                                .delete(subscription);
                    }

                } catch (Exception e) {

                    System.out.println(
                            "해당 구독 Push 전송 실패"
                    );

                    e.printStackTrace();
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "PushService 초기화 실패"
            );

            e.printStackTrace();
        }
    }
}