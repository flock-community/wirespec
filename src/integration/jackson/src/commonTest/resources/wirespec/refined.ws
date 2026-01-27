type StringRefinedRegex = String(/^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}$/g)
type StringRefined = String

type IntRefinedNoBound = Integer(_,_)
type IntRefinedLowerBound = Integer(-1,_)
type IntRefinedUpperound = Integer(_,2)
type IntRefinedLowerAndUpper = Integer(3,4)

type NumberRefinedNoBound = Number(_,_)
type NumberRefinedLowerBound = Number(-1.0,_)
type NumberRefinedUpperound = Number(_,2.0)
type NumberRefinedLowerAndUpper = Number(3.0,4.0)

type TypeWithAllRefined {
    stringRefinedRegex : StringRefinedRegex,
    stringRefined : StringRefined,
    intRefinedNoBound : IntRefinedNoBound,
    intRefinedLowerBound : IntRefinedLowerBound,
    intRefinedUpperound : IntRefinedUpperound,
    intRefinedLowerAndUpper : IntRefinedLowerAndUpper,
    numberRefinedNoBound : NumberRefinedNoBound,
    numberRefinedLowerBound : NumberRefinedLowerBound,
    numberRefinedUpperound : NumberRefinedUpperound,
    numberRefinedLowerAndUpper : NumberRefinedLowerAndUpper
}
