@me.tatarka.inject.annotations.Component
abstract class AppComponent {
    abstract val repo: Repository

}

@me.tatarka.inject.annotations.Inject
class Repository()
