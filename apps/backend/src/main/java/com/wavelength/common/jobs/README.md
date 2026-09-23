# Background jobs

Spring Scheduling is enabled; no job runs yet. Future jobs: MusicSyncJob,
TasteProfileRecalculationJob, MatchRecalculationJob, ConcertImportJob, WeeklyRecapJob.
Add a concrete `@Component` with a configurable `@Scheduled` method when a real
workflow exists. Use bounded batches, idempotent writes, timeouts, provider quota
handling and a distributed lock before deploying multiple backend replicas.
Never pass credentials to logs. Do not execute stub provider syncs.
