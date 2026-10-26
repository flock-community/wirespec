package community.flock.wirespec.compiler.test

public object CompileNestedValidationTest : Fixture {

    override val source: String =
        // language=ws
        """
        |type Tag = String(/^[a-z]+$/g)
        |type Score = Integer(0,100)
        |
        |type Label {
        |  name: Tag,
        |  code: String(/^[A-Z]{3}$/g),
        |  aliases: String(/^[a-z]+$/g)[]
        |}
        |
        |type Board {
        |  owner: Tag?,
        |  tags: Tag[]?,
        |  slots: Tag?[]?,
        |  grid: Score[][],
        |  tagsByKey: {Tag},
        |  scoresByKey: {Score[]},
        |  label: Label?,
        |  labels: Label[][],
        |  labelsByKey: {Label},
        |  limit: Integer(1,10),
        |  ratios: Number(0.0,1.0)[]
        |}
        |
        |type TreeNode {
        |  tag: Tag,
        |  children: TreeNode[]
        |}
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)
}
