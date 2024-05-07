import arrow.core.Ior
import arrow.typeclasses.Monoid.Companion.ListMonoid.combine
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Type


object Compare{

    fun compare(left:AST, right: AST){

    }

    fun compare(left: Type, right: Type){
       val leftIor = left.shape.value.map { Ior.Left(it) }
       val rightIor = left.shape.value.map { Ior.Left(it) }
        leftIor.combine(rightIor).
        left.shape.value.comb
        (leftIorzip rightIor).groupBy{left,right->
            Ior.Both("", "")
        }
    }

}
