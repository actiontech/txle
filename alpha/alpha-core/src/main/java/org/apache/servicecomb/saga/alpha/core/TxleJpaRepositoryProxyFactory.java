package org.apache.servicecomb.saga.alpha.core;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import javax.persistence.EntityManager;
import java.io.Serializable;

public class TxleJpaRepositoryProxyFactory<R extends JpaRepository<T, I>, T, I extends Serializable> extends JpaRepositoryFactoryBean<R, T, I> {
    public TxleJpaRepositoryProxyFactory(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Autowired
    TxleJpaRepositoryInterceptor txleJpaRepositoryInterceptor;

    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        try {
            JpaRepositoryFactory jpaFac = new JpaRepositoryFactory(entityManager);
            jpaFac.addRepositoryProxyPostProcessor((proxyFactory, repositoryInformation) -> proxyFactory.addAdvice((MethodInterceptor) methodInvocation -> txleJpaRepositoryInterceptor.doFilter(methodInvocation)));
            return jpaFac;
        } catch (Exception e) {
            return super.createRepositoryFactory(entityManager);
        }
    }
}
