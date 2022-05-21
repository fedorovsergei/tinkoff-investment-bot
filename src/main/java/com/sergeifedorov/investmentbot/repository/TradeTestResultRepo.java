package com.sergeifedorov.investmentbot.repository;

import com.sergeifedorov.investmentbot.domain.entity.TradeTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeTestResultRepo extends JpaRepository<TradeTestResult, Integer> {
}
