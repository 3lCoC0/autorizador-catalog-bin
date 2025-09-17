package com.credibanco.authorizer_catalog_bin_manager_cf.application;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.BinRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
    @Bean CreateBinUseCase createBinUseCase(BinRepository repo) { return new CreateBinService(repo); }
    @Bean ChangeBinStatusUseCase changeBinStatusUseCase(BinRepository repo) { return new ChangeBinStatusService(repo); }
    @Bean GetBinUseCase getBinUseCase(BinRepository repo) { return new GetBinService(repo); }
    @Bean ListBinsUseCase listBinsUseCase(BinRepository repo) { return new ListBinsService(repo); }
}
