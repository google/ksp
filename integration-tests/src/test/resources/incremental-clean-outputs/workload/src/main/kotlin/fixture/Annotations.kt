package fixture

annotation class GenerateModule(val version: Int = 1)
annotation class ConsumeModules(val expectPresent: Boolean = true)
annotation class CachedModule
annotation class CheckNextRound(val expectPresent: Boolean)
