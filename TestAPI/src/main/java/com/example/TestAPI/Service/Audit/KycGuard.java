package com.example.TestAPI.Service.Audit;

import com.example.TestAPI.Model.Enum.KycStatus;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.exception.BusinessException;
import com.example.TestAPI.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class KycGuard {

    public void requireVerified(User user) {
        if (user.getKycStatus() == KycStatus.VERIFIED) {
            return;
        }

        String message;
        if (user.getKycStatus() == KycStatus.PENDING) {
            message = "Votre vérification d'identité est en cours. Veuillez patienter.";
        } else if (user.getKycStatus() == KycStatus.REJECTED) {
            message = "Votre vérification d'identité a été rejetée. Veuillez soumettre à nouveau vos documents.";
        } else {
            message = "Pour effectuer cette action, vous devez d'abord vérifier votre identité. " +
                      "Cette vérification rapide protège la communauté. [Compléter ma vérification]";
        }
        throw new BusinessException(message, ErrorCode.FORBIDDEN);
    }
}
