package com.findmycoach.favorite;

import com.findmycoach.common.exception.ApiException;
import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.trainer.TrainerStatus;
import com.findmycoach.trainer.dto.TrainerResponse;
import com.findmycoach.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final TrainerRepository trainerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TrainerResponse> list(UUID userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(Favorite::getTrainer)
                .filter(t -> t.getStatus() == TrainerStatus.ACTIVE)
                .map(TrainerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<String> ids(UUID userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(f -> f.getTrainer().getId()).toList();
    }

    @Transactional
    public void add(UUID userId, String trainerId) {
        if (favoriteRepository.findByUserIdAndTrainerId(userId, trainerId).isPresent()) return;
        Trainer t = trainerRepository.findById(trainerId)
                .filter(x -> x.getStatus() == TrainerStatus.ACTIVE)
                .orElseThrow(() -> ApiException.notFound("Coach not found"));
        Favorite f = new Favorite();
        f.setUser(userRepository.getReferenceById(userId));
        f.setTrainer(t);
        favoriteRepository.save(f);
    }

    @Transactional
    public void remove(UUID userId, String trainerId) {
        favoriteRepository.findByUserIdAndTrainerId(userId, trainerId).ifPresent(favoriteRepository::delete);
    }
}
