package com.financeflow.service;

import com.financeflow.dto.SavingsGoalProgressRequest;
import com.financeflow.dto.SavingsGoalRequest;
import com.financeflow.dto.SavingsGoalResponse;
import com.financeflow.entity.SavingsGoal;
import com.financeflow.entity.User;
import com.financeflow.repository.SavingsGoalRepository;
import com.financeflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SavingsGoalService {
    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository, UserRepository userRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getAll(String email) {
        User user = getUser(email);
        return savingsGoalRepository.findAllByUser_IdOrderByTargetDateAscIdAsc(user.getId()).stream()
                .map(SavingsGoalService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse getById(String email, Long id) {
        User user = getUser(email);
        return toResponse(findGoal(id, user.getId()));
    }

    @Transactional
    public SavingsGoalResponse create(String email, SavingsGoalRequest request) {
        User user = getUser(email);
        SavingsGoal goal = new SavingsGoal();
        apply(goal, request, user);
        return toResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public SavingsGoalResponse update(String email, Long id, SavingsGoalRequest request) {
        User user = getUser(email);
        SavingsGoal goal = findGoal(id, user.getId());
        apply(goal, request, user);
        return toResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public SavingsGoalResponse updateProgress(String email, Long id, SavingsGoalProgressRequest request) {
        User user = getUser(email);
        SavingsGoal goal = findGoal(id, user.getId());
        goal.setCurrentAmount(request.currentAmount());
        return toResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public void delete(String email, Long id) {
        User user = getUser(email);
        savingsGoalRepository.delete(findGoal(id, user.getId()));
    }

    private SavingsGoal findGoal(Long id, Long userId) {
        return savingsGoalRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found"));
    }

    private static void apply(SavingsGoal goal, SavingsGoalRequest request, User user) {
        goal.setName(request.name().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(request.currentAmount());
        goal.setTargetDate(request.targetDate());
        goal.setUser(user);
    }

    private static SavingsGoalResponse toResponse(SavingsGoal goal) {
        BigDecimal progress = goal.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        return new SavingsGoalResponse(goal.getId(), goal.getName(), goal.getTargetAmount(),
                goal.getCurrentAmount(), goal.getTargetDate(), progress);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found"));
    }
}
