package com.healthmetrix.deident

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.fhirpath.IFhirPath
import io.micrometer.core.aop.CountedAspect
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.security.SecureRandom

@Configuration
class GlobalConfiguration {

    @Bean
    fun provideTaskExecutor(): TaskExecutor = ThreadPoolTaskExecutor().apply {
        setQueueCapacity(0)
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(60)
        initialize()
    }

    @Bean("applicationEventMulticaster")
    fun provideApplicationEventMulticaster(
        taskExecutor: TaskExecutor,
    ): ApplicationEventMulticaster = SimpleApplicationEventMulticaster().apply {
        setTaskExecutor(taskExecutor)
    }

    @Bean
    fun provideFhirContext(): FhirContext = FhirContext.forR4()

    @Bean
    fun provideFhirPath(fhirContext: FhirContext): IFhirPath = fhirContext.newFhirPath()

    @Bean
    fun provideRandom(): SecureRandom = SecureRandom()

    @Bean
    fun timedAspect(registry: MeterRegistry): TimedAspect = TimedAspect(registry)

    @Bean
    fun countedAspect(registry: MeterRegistry): CountedAspect = CountedAspect(registry)
}
