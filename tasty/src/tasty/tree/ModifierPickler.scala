package tasty.tree

abstract class ModifierPickler extends TreeSectionPickler {
  type Modifier

  def pickleModifier(modifier: Modifier): Unit
}
