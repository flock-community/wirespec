type Custom {
    custom: String
}

type StringRefinedRegex = String(/^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}$/g)
type StringRefined = String

type IntRefinedNoBound = Integer(_,_)
type IntRefinedLowerBound = Integer(-1,_)
type IntRefinedUpperBound = Integer(_,2)
type IntRefinedLowerAndUpper = Integer(3,4)

type NumberRefinedNoBound = Number(_,_)
type NumberRefinedLowerBound = Number(-1.0,_)
type NumberRefinedUpperBound = Number(_,2.0)
type NumberRefinedLowerAndUpper = Number(3.0,4.0)
