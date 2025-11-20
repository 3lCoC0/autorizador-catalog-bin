package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntityId;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.AgencyJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.TransactionDefinition;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaAgencyRepositoryTest {

    @Test
    void saveSetsAuditFieldsWhenMissing() {
        AgencyJpaRepository repository = mock(AgencyJpaRepository.class);
        JpaAgencyRepository jpaAgencyRepository = new JpaAgencyRepository(repository, new NoOpTransactionManager());

        Agency agency = Agency.rehydrate(
                "SUB", "AG", "Agency Name", "nit", "address", "phone", "dane",
                "highlight", "pins", "ccp", "ccpid", "ccs", "ccsid", "pcp", "pcpid", "pcs", "pcsid",
                "description", "A", null, null, "user"
        );

        when(repository.save(any())).thenAnswer(invocation -> {
            AgencyEntity entity = invocation.getArgument(0);
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getUpdatedAt()).isNotNull();
            return entity;
        });

        StepVerifier.create(jpaAgencyRepository.save(agency))
                .assertNext(saved -> {
                    assertThat(saved.subtypeCode()).isEqualTo("SUB");
                    assertThat(saved.agencyCode()).isEqualTo("AG");
                    assertThat(saved.createdAt()).isNotNull();
                    assertThat(saved.updatedAt()).isNotNull();
                })
                .verifyComplete();

        ArgumentCaptor<AgencyEntity> captor = ArgumentCaptor.forClass(AgencyEntity.class);
        Mockito.verify(repository).save(captor.capture());
        AgencyEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getId()).isEqualTo(new AgencyEntityId("SUB", "AG"));
        assertThat(savedEntity.getCreatedAt()).isInstanceOf(OffsetDateTime.class);
        assertThat(savedEntity.getUpdatedAt()).isInstanceOf(OffsetDateTime.class);
    }

    private static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // no-op
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // no-op
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // no-op
        }
    }
}
