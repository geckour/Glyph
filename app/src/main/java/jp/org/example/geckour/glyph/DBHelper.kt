package jp.org.example.geckour.glyph

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import android.util.Log

class DBHelper(context: Context) : SQLiteOpenHelper(context, DBHelper.DB_NAME, null, DBHelper.DB_VERSION) {

    private val SHAPERS = arrayOf(
        arrayOf("ACCEPT", "8,2,1,8"),
        arrayOf("ADVANCE", "5,3,9"),
        arrayOf("AGAIN", "9,3,2,0,4,1"),
        arrayOf("ALL", "5,6,7,8,9,10,5"),
        arrayOf("ANSWER", "3,4,1,0"),
        arrayOf("ATTACK", "9,3,5,4,7"),
        arrayOf("AVOID", "10,5,4,6,1"),
        arrayOf("BALANCE", "5,0,2,9,8,7,1,0"),
        arrayOf("BARRIER", "5,0,1,7"),
        arrayOf("BEGIN", "5,2,8,1"),
        arrayOf("BODY", "0,3,4,0"),
        arrayOf("BREATHE", "10,3,0,4,6"),
        arrayOf("CAPTURE", "6,1,0,2,9,8"),
        arrayOf("CHANGE", "2,0,8,1"),
        arrayOf("CHAOS", "9,10,5,6,4,0,2,8"),
        arrayOf("CIVILIZATION", "10,3,2,1,4,6"),
        arrayOf("CLEAR", "5,0,8"),
        arrayOf("CLEAR ALL", "5,0,8,9,10,5,6,7,8"),
        arrayOf("COLLECTIVE", "9,2,3,5,4,1,7"),
        arrayOf("COMPLEX", "4,3,0,2"),
        arrayOf("CONFLICT", "9,3,2,1,4,7"),
        arrayOf("CONTEMPLATE", "5,6,7,8,2,3,0,4"),
        arrayOf("CONTRACT", "1,4,7"),
        arrayOf("COURAGE", "9,3,2,1"),
        arrayOf("CREATE", "9,2,0,4,6"),
        arrayOf("CREATIVITY", "2,9,10,3,0,1,7,6,4"),
        arrayOf("DANGER", "5,3,0,8"),
        arrayOf("DATA", "5,4,0,2,8"),
        arrayOf("DEFEND", "10,2,8,1,6"),
        arrayOf("DESTINY", "3,0,4,1,2,8"),
        arrayOf("DESTROY", "10,3,0,1,7"),
        arrayOf("DETERIORATE", "3,0,2,9"),
        arrayOf("DIE", "9,2,0,1,7"),
        arrayOf("DIFFICULT", "2,0,1,4,6"),
        arrayOf("DISCOVER", "6,7,8,9"),
        arrayOf("DISTANCE", "5,10,9"),
        arrayOf("EASY", "4,0,2,8"),
        arrayOf("END", "8,0,5,6,1,8"),
        arrayOf("ENLIGHTENMENT", "8,7,6,5,3,0,4,3"),
        arrayOf("EQUAL", "2,3,4,1"),
        arrayOf("ESCAPE", "5,6,4,3,2"),
        arrayOf("EVOLUTION", "5,0,3,2"),
        arrayOf("FAILURE", "5,0,4,1"),
        arrayOf("FEAR", "3,4,1,6"),
        arrayOf("FOLLOW", "5,4,6,7"),
        arrayOf("FORGET", "2,9"),
        arrayOf("FUTURE", "6,4,1,7"),
        arrayOf("GAIN", "10,2"),
        arrayOf("GROW", "9,3,2"),
        arrayOf("HARM", "7,1,0,3,5,4,0"),
        arrayOf("HARMONY", "0,1,8,2,0,4,5,3,0"),
        arrayOf("HAVE", "1,0,2,8"),
        arrayOf("HELP", "10,3,0,2,1"),
        arrayOf("HIDE", "3,4,6,1,2"),
        arrayOf("HUMAN", "8,2,3,4,1,8"),
        arrayOf("I", "8,3,4,8"),
        arrayOf("IDEA", "2,9,10,3,0,1,7,6,4"),
        arrayOf("IGNORE", "1,7"),
        arrayOf("IMPERFECT", "0,3,2,0,4,2"),
        arrayOf("IMPROVE", "6,4,0,1"),
        arrayOf("IMPURE", "8,0,3,2,0"),
        arrayOf("INDIVIDUAL", "9,8,7"),
        arrayOf("INSIDE", "3,4,1"),
        arrayOf("JOURNEY", "6,4,0,3,10,9,8"),
        arrayOf("KNOWLEDGE", "8,3,0,4,8"),
        arrayOf("LEAD", "5,10,9,2,8"),
        arrayOf("LESS", "3,0,4"),
        arrayOf("LIBERATE", "9,3,0,4,6,5"),
        arrayOf("LIE", "2,3,0,1,4,0"),
        arrayOf("LIVE", "10,3,0,4,6"),
        arrayOf("LOSE", "6,1"),
        arrayOf("ME", "8,3,4,8"),
        arrayOf("MESSAGE", "9,3,0,1,6"),
        arrayOf("MIND", "8,2,3,0,8"),
        arrayOf("MORE", "2,0,1"),
        arrayOf("NATURE", "9,2,3,4,1,7"),
        arrayOf("NEW", "4,1,7"),
        arrayOf("NOT", "3,4,1"),
        arrayOf("NOURISH", "8,9,2,0,8"),
        arrayOf("NOW", "3,2,1,4"),
        arrayOf("OLD", "10,3,2"),
        arrayOf("OPEN", "8,2,1,8"),
        arrayOf("OPEN ALL", "8,2,1,8,9,10,5,6,7,8"),
        arrayOf("OUTSIDE", "5,10,9"),
        arrayOf("PAST", "10,3,2,9"),
        arrayOf("PATH", "5,0,2,9"),
        arrayOf("PEACE", "0,1,8,2,0,4,5,3,0"),
        arrayOf("PERFECTION", "5,0,2,9,8,7,1,0"),
        arrayOf("PORTAL", "10,3,4,6,7,1,2,9,10"),
        arrayOf("POTENTIAL", "5,0,1,7,6"),
        arrayOf("PRESENT", "3,2,1,4"),
        arrayOf("PROGRESS", "5,0,3,2"),
        arrayOf("PURE", "5,0,4,1,0"),
        arrayOf("PURSUE", "10,3,5,4"),
        arrayOf("QUESTION", "5,4,3,2"),
        arrayOf("REACT", "4,3,0,1,7"),
        arrayOf("REBEL", "10,2,0,4,6,7"),
        arrayOf("RECHARGE", "0,3,10,5,0"),
        arrayOf("REDUCE", "1,4,7"),
        arrayOf("REPAIR", "0,3,10,5,0"),
        arrayOf("REPEAT", "9,3,2,0,4,1"),
        arrayOf("RESISTANCE", "4,3,5,0,8,2"),
        arrayOf("RESTRAINT", "10,3,0,1,7,8"),
        arrayOf("RETREAT", "5,4,7"),
        arrayOf("SAFETY", "9,3,4,7"),
        arrayOf("SAVE", "2,0,1,6"),
        arrayOf("SEARCH", "0,4,3,2,1"),
        arrayOf("SEE", "5,3"),
        arrayOf("SELF", "9,8,7"),
        arrayOf("SEPARATE", "10,3,2,0,4,1,7"),
        arrayOf("SHAPERS", "9,2,3,5,4,1,7"),
        arrayOf("SHARE", "7,1,2,9,8"),
        arrayOf("SIMPLE", "2,1"),
        arrayOf("SOUL", "8,0,4,1,8"),
        arrayOf("STABILITY", "9,2,1,7"),
        arrayOf("STAY", "9,2,1,7"),
        arrayOf("STRONG", "2,3,4,1,2"),
        arrayOf("STRUGGLE", "4,3,5,0,8,2"),
        arrayOf("SUCCESS", "5,0,3,2"),
        arrayOf("THEM", "5,2,1"),
        arrayOf("THOUGHT", "2,9,10,3,0,1,7,6,4"),
        arrayOf("TOGETHER", "9,2,0,4,3,0"),
        arrayOf("TRUTH", "3,0,1,4,0,2,3"),
        arrayOf("US", "3,4,8"),
        arrayOf("USE", "0,1,6"),
        arrayOf("WANT", "9,2,8,1"),
        arrayOf("WAR", "9,3,5,4,7"),
        arrayOf("WE", "3,4,8"),
        arrayOf("WEAK", "10,3,4,1"),
        arrayOf("XM", "2,3,4,1,0,2"),
        arrayOf("YOU", "5,2,1,5"),
        arrayOf("YOUR", "5,2,1,5")
    )

    private val SETS = arrayOf(
        arrayOf("5", "ADVANCE,CIVILIZATION,PURSUE,SHAPERS,PATH", null),
        arrayOf("5", "ADVANCE,CIVILIZATION,PURSUE,SHAPERS,TRUTH", null),
        arrayOf("5", "ANSWER,QUESTION,DISCOVER,DIFFICULT,TRUTH", null),
        arrayOf("5", "ATTACK,HUMAN,CIVILIZATION,XM,MESSAGE", null),
        arrayOf("5", "ATTACK,PURE,FUTURE,HUMAN,CIVILIZATION", null),
        arrayOf("5", "ATTACK,PURE,FUTURE,INSIDE,WAR", null),
        arrayOf("5", "ATTACK,SEPARATE,PATH,END,JOURNEY", null),
        arrayOf("5", "AVOID,CHAOS,AVOID,SHAPERS,LIE", null),
        arrayOf("5", "AVOID,CHAOS,REPAIR,POTENTIAL,WAR", null),
        arrayOf("5", "AVOID,PERFECTION,STAY,HUMAN,SELF", null),
        arrayOf("5", "BREATHE,INSIDE,XM,LOSE,SELF", null),
        arrayOf("5", "CAPTURE,PORTAL,DEFEND,PORTAL,COURAGE", null),
        arrayOf("5", "CHANGE,SEPARATE,PAST,END,JOURNEY", null),
        arrayOf("5", "CHAOS,ATTACK,CONFLICT,DISCOVER,HARMONY", null),
        arrayOf("5", "CHAOS,PERFECTION,STABILITY,HUMAN,SELF", null),
        arrayOf("5", "CLEAR,MIND,LIBERATE,BARRIER,BODY", null),
        arrayOf("5", "CLEAR ALL,MIND,PAST,PRESENT,FUTURE", null),
        arrayOf("5", "CLEAR ALL,MIND,LIBERATE,BARRIER,BODY", null),
        arrayOf("5", "CONTEMPLATE,FUTURE,NOT,SHAPERS,PATH", null),
        arrayOf("5", "CONTEMPLATE,RESTRAINT,DISCOVER,MORE,COURAGE", null),
        arrayOf("5", "COURAGE,ATTACK,SHAPERS,PORTAL,TOGETHER", null),
        arrayOf("5", "COURAGE,DESTROY,SHAPERS,PORTAL,TOGETHER", null),
        arrayOf("5", "CREATE,PURE,FUTURE,HUMAN,CIVILIZATION", null),
        arrayOf("5", "CREATE,PURE,FUTURE,NOT,WAR", null),
        arrayOf("5", "CREATE,SEPARATE,PATH,END,JOURNEY", null),
        arrayOf("5", "DEFEND,DESTINY,DEFEND,HUMAN,CIVILIZATION", null),
        arrayOf("5", "DEFEND,HUMAN,CIVILIZATION,PORTAL,MESSAGE", null),
        arrayOf("5", "DEFEND,HUMAN,CIVILIZATION,SHAPERS,LIE", null),
        arrayOf("5", "DEFEND,HUMAN,CIVILIZATION,SHAPERS,PORTAL", null),
        arrayOf("5", "DEFEND,HUMAN,CIVILIZATION,XM,MESSAGE", null),
        arrayOf("5", "DESTROY,HUMAN,CIVILIZATION,SHAPERS,LIE", null),
        arrayOf("5", "DESTROY,CIVILIZATION,END,CONFLICT,WAR", null),
        arrayOf("5", "DESTROY,CIVILIZATION,NOW,CONFLICT,WAR", null),
        arrayOf("5", "DESTROY,LIE,INSIDE,GAIN,SOUL", null),
        arrayOf("5", "DISTANCE,SELF,AVOID,HUMAN,LIE", null),
        arrayOf("5", "EASY,PAST,FUTURE,FOLLOW,SHAPERS", null),
        arrayOf("5", "ESCAPE,BODY,JOURNEY,OUTSIDE,NOW", null),
        arrayOf("5", "FORGET,WAR,SEE,DISTANCE,HARMONY", null),
        arrayOf("5", "FORGET,WAR,GAIN,DISTANCE,HARMONY", null),
        arrayOf("5", "FORGET,PAST,SEE,PRESENT,DANGER", null),
        arrayOf("5", "FORGET,WAR,SEE,DISTANCE,HARMONY", null),
        arrayOf("5", "GAIN,TRUTH,OPEN,HUMAN,SOUL", null),
        arrayOf("5", "HARM,PROGRESS,PURSUE,MORE,WAR", null),
        arrayOf("5", "HELP,ENLIGHTENMENT,CAPTURE,ALL,PORTAL", null),
        arrayOf("5", "HELP,HUMAN,CIVILIZATION,PURSUE,DESTINY", null),
        arrayOf("5", "HELP,RESISTANCE,CAPTURE,ALL,PORTAL", null),
        arrayOf("5", "HIDE,RESISTANCE,ADVANCE,STRONG,TOGETHER", null),
        arrayOf("5", "HUMAN,NOT,TOGETHER,CIVILIZATION,DETERIORATE", null),
        arrayOf("5", "HUMAN,SHAPERS,TOGETHER,CREATE,DESTINY", null),
        arrayOf("5", "IMPERFECT,TRUTH,OPEN,COMPLEX,ANSWER", null),
        arrayOf("5", "IMPERFECT,XM,MESSAGE,HUMAN,CHAOS", null),
        arrayOf("5", "IMPROVE,MIND,IMPROVE,COURAGE,CHANGE", null),
        arrayOf("5", "INSIDE,MIND,INSIDE,SOUL,HARMONY", null),
        arrayOf("5", "LIBERATE,PORTAL,LIBERATE,HUMAN,MIND", null),
        arrayOf("5", "LIBERATE,SELF,LIBERATE,HUMAN,CIVILIZATION", null),
        arrayOf("5", "LIVE,INSIDE,XM,LOSE,SELF", null),
        arrayOf("5", "LOSE,SHAPERS,MESSAGE,GAIN,CHAOS", null),
        arrayOf("5", "MIND,BODY,SOUL,PURE,HUMAN", null),
        arrayOf("5", "MORE,DATA,GAIN,PORTAL,ADVANCE", null),
        arrayOf("5", "OLD,NATURE,LESS,STRONG,NOW", null),
        arrayOf("5", "OUTSIDE,SELF,AVOID,HUMAN,LIE", null),
        arrayOf("5", "PAST,BARRIER,CREATE,FUTURE,JOURNEY", null),
        arrayOf("5", "PAST,CHAOS,CREATE,FUTURE,HARMONY", null),
        arrayOf("5", "PAST,PATH,CREATE,FUTURE,JOURNEY", null),
        arrayOf("5", "PORTAL,ATTACK,DANGER,PURSUE,SAFETY", null),
        arrayOf("5", "PORTAL,BARRIER,DEFEND,HUMAN,SHAPERS", null),
        arrayOf("5", "PORTAL,CREATE,DANGER,PURSUE,SAFETY", null),
        arrayOf("5", "PORTAL,IMPROVE,HUMAN,FUTURE,CIVILIZATION", null),
        arrayOf("5", "PORTAL,POTENTIAL,HELP,HUMAN,FUTURE", null),
        arrayOf("5", "PRESENT,CHAOS,CREATE,FUTURE,CIVILIZATION", null),
        arrayOf("5", "PURSUE,CONFLICT,ADVANCE,WAR,CHAOS", null),
        arrayOf("5", "PURSUE,CONFLICT,WAR,ADVANCE,CHAOS", null),
        arrayOf("5", "PURSUE,PATH,OUTSIDE,SHAPERS,LIE", null),
        arrayOf("5", "QUESTION,HUMAN,CIVILIZATION,DESTROY,PORTAL", null),
        arrayOf("5", "QUESTION,LESS,FORGET,ALL,LIE", null),
        arrayOf("5", "REACT,REBEL,QUESTION,SHAPERS,LIE", null),
        arrayOf("5", "REBEL,IDEA,EVOLUTION,DESTINY,HARMONY", null),
        arrayOf("5", "REBEL,IDEA,EVOLUTION,DESTINY,NOW", null),
        arrayOf("5", "REPAIR,INSIDE,REPAIR,HUMAN,SOUL", null),
        arrayOf("5", "REPAIR,PRESENT,REPAIR,HUMAN,SOUL", null),
        arrayOf("5", "REPAIR,SOUL,LESS,HUMAN,HARM", null),
        arrayOf("5", "SAVE,HUMAN,CIVILIZATION,DESTROY,PORTAL", null),
        arrayOf("5", "SEARCH,DESTINY,CREATE,PURE,FUTURE", null),
        arrayOf("5", "SEPARATE,MIND,BODY,DISCOVER,ENLIGHTENMENT", null),
        arrayOf("5", "SEPARATE,TRUTH,LIE,SHAPERS,FUTURE", null),
        arrayOf("5", "SHAPERS,LEAD,HUMAN,COMPLEX,JOURNEY", null),
        arrayOf("5", "SHAPERS,PORTAL,DATA,CREATE,CHAOS", null),
        arrayOf("5", "SHAPERS,PORTAL,MESSAGE,DESTROY,CIVILIZATION", null),
        arrayOf("5", "SHAPERS,SEE,COMPLEX,PATH,DESTINY", null),
        arrayOf("5", "SHAPERS,WANT,HUMAN,MIND,FUTURE", null),
        arrayOf("5", "SIMPLE,OLD,TRUTH,JOURNEY,INSIDE", null),
        arrayOf("5", "SIMPLE,TRUTH,FORGET,EASY,SUCCESS", null),
        arrayOf("5", "SIMPLE,TRUTH,SHAPERS,DESTROY,CIVILIZATION", null),
        arrayOf("5", "STAY,STRONG,TOGETHER,DEFEND,RESISTANCE", null),
        arrayOf("5", "STRONG,TOGETHER,ATTACK,TOGETHER,DESTINY", null),
        arrayOf("5", "USE,MIND,USE,COURAGE,CHANGE", null),
        arrayOf("5", "USE,RESTRAINT,FOLLOW,EASY,PATH", null),
        arrayOf("5", "WANT,TRUTH,PURSUE,DIFFICULT,PATH", null),
        arrayOf("5", "WEAK,HUMAN,DESTINY,DESTROY,CIVILIZATION", null),
        arrayOf("5", "XM,CREATE,COMPLEX,HUMAN,DESTINY", null),
        arrayOf("5", "XM,PAST,FUTURE,DESTINY,HARMONY", null),
        arrayOf("5", "XM,PATH,FUTURE,JOURNEY,HARMONY", null), //#4
        arrayOf("4", "ADVANCE,FUTURE,NOT,WAR", null),
        arrayOf("4", "ADVANCE,CIVILIZATION,REPEAT,FAILURE", null),
        arrayOf("4", "ALL,CHAOS,INSIDE,BODY", null),
        arrayOf("4", "ATTACK,CIVILIZATION,REPEAT,FAILURE", null),
        arrayOf("4", "ATTACK,ENLIGHTENMENT,PURSUE,RESISTANCE", null),
        arrayOf("4", "ATTACK,FUTURE,CHANGE,DESTINY", null),
        arrayOf("4", "ATTACK,RESISTANCE,PURSUE,ENLIGHTENMENT", null),
        arrayOf("4", "ATTACK,WEAK,SHAPERS,LIE", null),
        arrayOf("4", "AVOID,XM,MESSAGE,LIE", null),
        arrayOf("4", "BREATHE,AGAIN,JOURNEY,AGAIN", null),
        arrayOf("4", "BREATHE,NATURE,PERFECTION,HARMONY", null),
        arrayOf("4", "CAPTURE,FEAR,DISCOVER,COURAGE", null),
        arrayOf("4", "CHANGE,BODY,IMPROVE,HUMAN", null),
        arrayOf("4", "CHANGE,FUTURE,CAPTURE,DESTINY", null),
        arrayOf("4", "CHANGE,HUMAN,POTENTIAL,USE", null),
        arrayOf("4", "CHANGE,SIMPLE,HUMAN,FUTURE", null),
        arrayOf("4", "CHAOS,BARRIER,SHAPERS,PORTAL", null),
        arrayOf("4", "CHAOS,DESTROY,SHAPERS,PORTAL", null),
        arrayOf("4", "CLEAR,MIND,OPEN,MIND", null),
        arrayOf("4", "CLEAR ALL,OPEN ALL,DISCOVER,TRUTH", null),
        arrayOf("4", "CLEAR ALL,OPEN ALL,GAIN,TRUTH", null),
        arrayOf("4", "CLEAR ALL,OPEN,MIND,BEGIN", null),
        arrayOf("4", "COMPLEX,SHAPERS,CIVILIZATION,STRONG", null),
        arrayOf("4", "CONTEMPLATE,COMPLEX,SHAPERS,CIVILIZATION", null),
        arrayOf("4", "CONTEMPLATE,COMPLEX,SHAPERS,TRUTH", null),
        arrayOf("4", "CONTEMPLATE,SELF,PATH,TRUTH", null),
        arrayOf("4", "COURAGE,WAR,SHAPERS,FUTURE", null),
        arrayOf("4", "CREATE,DISTANCE,IMPURE,PATH", null),
        arrayOf("4", "CREATE,FUTURE,CHANGE,DESTINY", null),
        arrayOf("4", "CREATE,FUTURE,NOT,WAR", null),
        arrayOf("4", "DEFEND,MESSAGE,ANSWER,IDEA", null),
        arrayOf("4", "DESTROY,COMPLEX,SHAPERS,LIE", null),
        arrayOf("4", "DESTROY,DESTINY,HUMAN,LIE", null),
        arrayOf("4", "DETERIORATE,HUMAN,WEAK,REBEL", null),
        arrayOf("4", "DISCOVER,PERFECTION,SAFETY,ALL", null),
        arrayOf("4", "DISTANCE,YOU,MIND,MORE", null),
        arrayOf("4", "END,JOURNEY,DISCOVER,DESTINY", null),
        arrayOf("4", "ESCAPE,SIMPLE,HUMAN,FUTURE", null),
        arrayOf("4", "FOLLOW,SHAPERS,PORTAL,MESSAGE", null),
        arrayOf("4", "FORGET,CONFLICT,ACCEPT,WAR", null),
        arrayOf("4", "GAIN,PORTAL,ATTACK,WEAK", null),
        arrayOf("4", "HARMONY,PATH,NOURISH,PRESENT", null),
        arrayOf("4", "HELP,GAIN,CREATE,PURSUE", null),
        arrayOf("4", "HELP,SHAPERS,CREATE,FUTURE", null),
        arrayOf("4", "HIDE,IMPURE,HUMAN,THOUGHT", null),
        arrayOf("4", "HUMAN,HAVE,IMPURE,CIVILIZATION", null),
        arrayOf("4", "HUMAN,PAST,PRESENT,FUTURE", null),
        arrayOf("4", "HUMAN,SOUL,STRONG,PURE", null),
        arrayOf("4", "IGNORE,HUMAN,CHAOS,LIE", null),
        arrayOf("4", "IMPROVE,BODY,MIND,SOUL", null),
        arrayOf("4", "IMPROVE,BODY,PURSUE,JOURNEY", null),
        arrayOf("4", "IMPROVE,MIND,BODY,INSIDE", null),
        arrayOf("4", "IMPROVE,MIND,JOURNEY,INSIDE", null),
        arrayOf("4", "INSIDE,MIND,JOURNEY,PERFECTION", null),
        arrayOf("4", "INSIDE,MIND,SOUL,HARMONY", null),
        arrayOf("4", "JOURNEY,INSIDE,IMPROVE,SOUL", null),
        arrayOf("4", "LEAD,BODY,MIND,SOUL", null),
        arrayOf("4", "LEAD,PURSUE,REACT,DEFEND", null),
        arrayOf("4", "LESS,CHAOS,MORE,STABILITY", null),
        arrayOf("4", "LESS,MIND,MORE,SOUL", null),
        arrayOf("4", "LESS,SOUL,MORE,MIND", null),
        arrayOf("4", "LESS,TRUTH,MORE,CHAOS", null),
        arrayOf("4", "LIBERATE,XM,PORTAL,TOGETHER", null),
        arrayOf("4", "LIVE,AGAIN,JOURNEY,AGAIN", null),
        arrayOf("4", "LIVE,NATURE,BALANCE,HARMONY", null),
        arrayOf("4", "LOSE,DANGER,GAIN,SAFETY", null),
        arrayOf("4", "MORE,MIND,LESS,SOUL", null),
        arrayOf("4", "NOT,MIND,JOURNEY,PERFECTION", null),
        arrayOf("4", "NOURISH,XM,CREATE,THOUGHT", null),
        arrayOf("4", "OPEN,CHAOS,INSIDE,BODY", null),
        arrayOf("4", "OPEN ALL,CLEAR ALL,DISCOVER,TRUTH", null),
        arrayOf("4", "PAST,AGAIN,PRESENT,AGAIN", null),
        arrayOf("4", "PATH,RESTRAINT,STRONG,SAFETY", null),
        arrayOf("4", "PEACE,PATH,NOURISH,FUTURE", null),
        arrayOf("4", "PEACE,PATH,NOURISH,NOW", null),
        arrayOf("4", "PERFECTION,BALANCE,SAFETY,ALL", null),
        arrayOf("4", "PORTAL,CHANGE,CIVILIZATION,END", null),
        arrayOf("4", "PORTAL,DIE,CIVILIZATION,DIE", null),
        arrayOf("4", "PORTAL,HAVE,TRUTH,DATA", null),
        arrayOf("4", "PORTAL,POTENTIAL,CHANGE,FUTURE", null),
        arrayOf("4", "PRESENT,MIND,JOURNEY,PERFECTION", null),
        arrayOf("4", "QUESTION,TRUTH,GAIN,FUTURE", null),
        arrayOf("4", "QUESTION,YOU,IMPURE,CIVILIZATION", null),
        arrayOf("4", "RESISTANCE,DEFEND,SHAPERS,DANGER", null),
        arrayOf("4", "RESTRAINT,FEAR,AVOID,DANGER", null),
        arrayOf("4", "RESTRAINT,PATH,GAIN,HARMONY", null),
        arrayOf("4", "SAVE,HUMAN,POTENTIAL,USE", null),
        arrayOf("4", "SEE,TRUTH,SEE,FUTURE", null),
        arrayOf("4", "SEARCH,DATA,DISCOVER,PATH", null),
        arrayOf("4", "SEARCH,TRUTH,SAVE,CIVILIZATION", null),
        arrayOf("4", "SEARCH,XM,SAVE,PORTAL", null),
        arrayOf("4", "SEARCH,TRUTH,SEE,FUTURE", null),
        arrayOf("4", "SEPARATE,WEAK,IGNORE,TRUTH", null),
        arrayOf("4", "SHAPERS,AVOID,PURE,THOUGHT", null),
        arrayOf("4", "SHAPERS,CHAOS,PURE,HARM", null),
        arrayOf("4", "SHAPERS,GAIN,POTENTIAL,EVOLUTION", null),
        arrayOf("4", "SHAPERS,HAVE,STRONG,PATH", null),
        arrayOf("4", "SHAPERS,LOSE,POTENTIAL,EVOLUTION", null),
        arrayOf("4", "SHAPERS,MESSAGE,END,CIVILIZATION", null),
        arrayOf("4", "SHAPERS,MIND,COMPLEX,HARMONY", null),
        arrayOf("4", "SHAPERS,PAST,PRESENT,FUTURE", null),
        arrayOf("4", "SHAPERS,PORTAL,MIND,RESTRAINT", null),
        arrayOf("4", "SHAPERS,SEE,POTENTIAL,EVOLUTION", null),
        arrayOf("4", "SIMPLE,CIVILIZATION,IMPURE,WEAK", null),
        arrayOf("4", "SIMPLE,MESSAGE,COMPLEX,IDEA", null),
        arrayOf("4", "SIMPLE,TRUTH,LIVE,NATURE", null),
        arrayOf("4", "SOUL,HUMAN,REBEL,DIE", null),
        arrayOf("4", "STABILITY,PURE,LIVE,KNOWLEDGE", null),
        arrayOf("4", "STAY,TOGETHER,DEFEND,TRUTH", null),
        arrayOf("4", "STRONG,IDEA,PURSUE,TRUTH", null),
        arrayOf("4", "STRONG,RESISTANCE,CAPTURE,PORTAL", null),
        arrayOf("4", "STRONG,TOGETHER,AVOID,WAR", null),
        arrayOf("4", "AVOID,DEFEND,SHAPERS,DANGER", null),
        arrayOf("4", "AVOID,IMPROVE,HUMAN,SOUL", null),
        arrayOf("4", "TOGETHER,DISCOVER,HARMONY,EQUAL", null),
        arrayOf("4", "TRUTH,IDEA,DISCOVER,XM", null),
        arrayOf("4", "XM,DIE,CHAOS,LIVE", null),
        arrayOf("4", "XM,HAVE,MIND,HARMONY", null),
        arrayOf("4", "XM,HAVE,MIND,JOURNEY", null),
        arrayOf("4", "YOU,DESTINY,NOT,EASY", null), //#3
        arrayOf("3", "ACCEPT,HUMAN,WEAK", null),
        arrayOf("3", "ADVANCE,HUMAN,ENLIGHTENMENT", ",,ENLIGHTENED"),
        arrayOf("3", "ADVANCE,HUMAN,RESISTANCE", null),
        arrayOf("3", "ADVANCE,PURE,TRUTH", null),
        arrayOf("3", "AGAIN,JOURNEY,OUTSIDE", null),
        arrayOf("3", "ALL,CIVILIZATION,CHAOS", null),
        arrayOf("3", "ALL,XM,LIBERATE", null),
        arrayOf("3", "ATTACK,CREATE,DANGER", null),
        arrayOf("3", "ATTACK,DESTROY,FUTURE", null),
        arrayOf("3", "ATTACK,DIFFICULT,FUTURE", null),
        arrayOf("3", "ATTACK,SHAPERS,CHAOS", null),
        arrayOf("3", "ATTACK,SHAPERS,EVOLUTION", null),
        arrayOf("3", "ATTACK,SHAPERS,PORTAL", null),
        arrayOf("3", "AVOID,ATTACK,CHAOS", null),
        arrayOf("3", "AVOID,CHAOS,SOUL", null),
        arrayOf("3", "AVOID,COMPLEX,CONFLICT", null),
        arrayOf("3", "AVOID,COMPLEX,SOUL", null),
        arrayOf("3", "AVOID,COMPLEX,TRUTH", null),
        arrayOf("3", "AVOID,DESTINY,LIE", null),
        arrayOf("3", "AVOID,IMPURE,EVOLUTION", null),
        arrayOf("3", "AVOID,PURE,CHAOS", null),
        arrayOf("3", "AVOID,WAR,CHAOS", null),
        arrayOf("3", "ANSWER,REPEAT,AVOID", null),
        arrayOf("3", "CAPTURE,SHAPERS,PORTAL", null),
        arrayOf("3", "CAPTURE,XM,PORTAL", null),
        arrayOf("3", "CHANGE,HUMAN,FUTURE", null),
        arrayOf("3", "CIVILIZATION,WAR,CHAOS", null),
        arrayOf("3", "COMPLEX,JOURNEY,FUTURE", null),
        arrayOf("3", "CONTEMPLATE,JOURNEY,OUTSIDE", null),
        arrayOf("3", "CONTEMPLATE,POTENTIAL,PERFECTION", null),
        arrayOf("3", "CONTEMPLATE,POTENTIAL,JOURNEY", null),
        arrayOf("3", "COURAGE,DESTINY,REBEL", null),
        arrayOf("3", "CREATE,FUTURE,JOURNEY", null),
        arrayOf("3", "DANGER,CHANGE,PAST", null),
        arrayOf("3", "DEFEND,SEARCH,SAFETY", null),
        arrayOf("3", "DESTROY,DESTINY,BARRIER", null),
        arrayOf("3", "DESTROY,DIFFICULT,BARRIER", null),
        arrayOf("3", "DESTROY,CIVILIZATION,DANGER", null),
        arrayOf("3", "DESTROY,IMPURE,TRUTH", null),
        arrayOf("3", "DESTROY,WEAK,CIVILIZATION", null),
        arrayOf("3", "DETERIORATE,ADVANCE,PRESENT", null),
        arrayOf("3", "DISCOVER,HARMONY,EQUAL", null),
        arrayOf("3", "DISCOVER,PORTAL,TRUTH", null),
        arrayOf("3", "DISCOVER,PURE,TRUTH", null),
        arrayOf("3", "DISCOVER,RESISTANCE,TRUTH", null),
        arrayOf("3", "DISCOVER,SAFETY,CIVILIZATION", null),
        arrayOf("3", "DISCOVER,SHAPERS,CIVILIZATION", null),
        arrayOf("3", "DISCOVER,SHAPERS,ENLIGHTENMENT", null),
        arrayOf("3", "DISCOVER,SHAPERS,LIE", null),
        arrayOf("3", "DISCOVER,SHAPERS,MESSAGE", null),
        arrayOf("3", "ESCAPE,IMPURE,EVOLUTION", null),
        arrayOf("3", "ESCAPE,IMPURE,FUTURE", null),
        arrayOf("3", "ESCAPE,IMPURE,TRUTH", null),
        arrayOf("3", "ESCAPE,PORTAL,HARM", null),
        arrayOf("3", "ESCAPE,SHAPERS,HARM", null),
        arrayOf("3", "ESCAPE,SHAPERS,HARMONY", null),
        arrayOf("3", "FEAR,CHAOS,XM", null),
        arrayOf("3", "FEAR,COMPLEX,XM", null),
        arrayOf("3", "FOLLOW,PURE,JOURNEY", null),
        arrayOf("3", "FUTURE,EQUAL,PAST", null),
        arrayOf("3", "GAIN,CIVILIZATION,PEACE", null),
        arrayOf("3", "GAIN,FUTURE,ESCAPE", null),
        arrayOf("3", "HARM,DANGER,AVOID", null),
        arrayOf("3", "HARMONY,STABILITY,FUTURE", null),
        arrayOf("3", "HIDE,JOURNEY,TRUTH", null),
        arrayOf("3", "HIDE,PATH,FUTURE", null),
        arrayOf("3", "HUMAN,GAIN,SAFETY", null),
        arrayOf("3", "IMPROVE,ADVANCE,PRESENT", null),
        arrayOf("3", "IMPROVE,FUTURE,TOGETHER", null),
        arrayOf("3", "IMPROVE,HUMAN,SHAPERS", null),
        arrayOf("3", "INSIDE,MIND,FUTURE", null),
        arrayOf("3", "INSIDE,XM,TRUTH", null),
        arrayOf("3", "JOURNEY,INSIDE,SOUL", null),
        arrayOf("3", "LEAD,ENLIGHTENMENT,CIVILIZATION", null),
        arrayOf("3", "LEAD,RESISTANCE,QUESTION", null),
        arrayOf("3", "LIBERATE,HUMAN,FUTURE", null),
        arrayOf("3", "LIBERATE,PORTAL,POTENTIAL", null),
        arrayOf("3", "LOSE,WAR,RETREAT", null),
        arrayOf("3", "MIND,BODY,LIVE", null),
        arrayOf("3", "MIND,EQUAL,TRUTH", null),
        arrayOf("3", "MIND,OPEN,LIVE", null),
        arrayOf("3", "NATURE,PURE,DEFEND", null),
        arrayOf("3", "NOURISH,MIND,JOURNEY", null),
        arrayOf("3", "NOURISH,XM,PORTAL", null),
        arrayOf("3", "OPEN,HUMAN,WEAK", null),
        arrayOf("3", "OPEN ALL,PORTAL,SUCCESS", null),
        arrayOf("3", "OPEN ALL,SIMPLE,TRUTH", null),
        arrayOf("3", "PAST,EQUAL,FUTURE", null),
        arrayOf("3", "PAST,HARMONY,DIFFICULT", null),
        arrayOf("3", "PAST,PEACE,DIFFICULT", null),
        arrayOf("3", "PAST,PRESENT,FUTURE", null),
        arrayOf("3", "PATH,PEACE,DIFFICULT", null),
        arrayOf("3", "PEACE,SIMPLE,JOURNEY", null),
        arrayOf("3", "PEACE,STABILITY,FUTURE", null),
        arrayOf("3", "PERFECTION,PAST,PEACE", null),
        arrayOf("3", "POTENTIAL,TRUTH,HARMONY", null),
        arrayOf("3", "POTENTIAL,XM,ATTACK", null),
        arrayOf("3", "POTENTIAL,XM,PEACE", null),
        arrayOf("3", "PURSUE,COMPLEX,TRUTH", null),
        arrayOf("3", "PURSUE,PURE,BODY", null),
        arrayOf("3", "QUESTION,CONFLICT,DATA", null),
        arrayOf("3", "QUESTION,HIDE,TRUTH", null),
        arrayOf("3", "QUESTION,HUMAN,TRUTH", null),
        arrayOf("3", "QUESTION,SHAPERS,CHAOS", null),
        arrayOf("3", "REACT,IMPURE,CIVILIZATION", null),
        arrayOf("3", "REACT,PURE,TRUTH", null),
        arrayOf("3", "REPAIR,NATURE,BALANCE", null),
        arrayOf("3", "REPEAT,JOURNEY,OUTSIDE", null),
        arrayOf("3", "REPEAT,SEARCH,SAFETY", null),
        arrayOf("3", "SEARCH,XM,PORTAL", null),
        arrayOf("3", "SEE,TRUTH,NOW", null),
        arrayOf("3", "SEPARATE,FUTURE,EVOLUTION", null),
        arrayOf("3", "SEPARATE,FUTURE,PURSUE", null),
        arrayOf("3", "TOGETHER,PURE,JOURNEY", null),
        arrayOf("3", "TOGETHER,PURSUE,SAFETY", null),
        arrayOf("3", "TRUTH,NOURISH,SOUL", null),
        arrayOf("3", "WANT,TRUTH,NOW", null),
        arrayOf("3", "WAR,ATTACK,CHAOS", null),
        arrayOf("3", "WAR,CREATE,DANGER", null),
        arrayOf("3", "WAR,DESTROY,FUTURE", null),
        arrayOf("3", "XM,NOURISH,CIVILIZATION", null), //#2
        arrayOf("2", "ADVANCE,ENLIGHTENMENT", null),
        arrayOf("2", "ATTACK,EVOLUTION", null),
        arrayOf("2", "AVOID,CONFLICT", null),
        arrayOf("2", "CAPTURE,PORTAL", null),
        arrayOf("2", "CHANGE,PRESENT", null),
        arrayOf("2", "ESCAPE,TOGETHER", null),
        arrayOf("2", "CIVILIZATION,CHAOS", null),
        arrayOf("2", "CIVILIZATION,WEAK", null),
        arrayOf("2", "CREATE,DANGER", null),
        arrayOf("2", "CREATE,FUTURE", null),
        arrayOf("2", "DEFEND,NATURE", null),
        arrayOf("2", "DIFFICULT,BARRIER", null),
        arrayOf("2", "DISCOVER,ENLIGHTENMENT", null),
        arrayOf("2", "DISCOVER,LIE", null),
        arrayOf("2", "DISCOVER,PORTAL", null),
        arrayOf("2", "DISCOVER,RESISTANCE", null),
        arrayOf("2", "ESCAPE,EVOLUTION", null),
        arrayOf("2", "FOLLOW,JOURNEY", null),
        arrayOf("2", "GAIN,PEACE", null),
        arrayOf("2", "GAIN,SAFETY", null),
        arrayOf("2", "HIDE,TRUTH", null),
        arrayOf("2", "IMPROVE,HUMAN", null),
        arrayOf("2", "JOURNEY,INSIDE", null),
        arrayOf("2", "LEAD,RESISTANCE", null),
        arrayOf("2", "LIBERATE,XM", null),
        arrayOf("2", "NOURISH,JOURNEY", null),
        arrayOf("2", "OPEN ALL,PORTAL", null),
        arrayOf("2", "OPEN ALL,TRUTH", null),
        arrayOf("2", "PATH,PEACE", null),
        arrayOf("2", "PATH,PERFECTION", null),
        arrayOf("2", "PEACE,STABILITY", null),
        arrayOf("2", "PURE,CHAOS", null),
        arrayOf("2", "PURE,LIE", null),
        arrayOf("2", "PURE,MIND", null),
        arrayOf("2", "PURE,SHAPERS", null),
        arrayOf("2", "PURE,TRUTH", null),
        arrayOf("2", "PURSUE,CONFLICT", null),
        arrayOf("2", "PURSUE,JOURNEY", null),
        arrayOf("2", "QUESTION,ALL", null),
        arrayOf("2", "QUESTION,CIVILIZATION", null),
        arrayOf("2", "QUESTION,TRUTH", null),
        arrayOf("2", "QUESTION,WAR", null),
        arrayOf("2", "RETREAT,SAFETY", null),
        arrayOf("2", "SEE,SHAPERS", null),
        arrayOf("2", "SEE,TRUTH", null),
        arrayOf("2", "SEARCH,POTENTIAL", null),
        arrayOf("2", "SEPARATE,WAR", null),
        arrayOf("2", "STRONG,BODY", null),
        arrayOf("2", "STRONG,MIND", null),
        arrayOf("2", "STRONG,SOUL", null)
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("create table if not exists $TABLE_NAME1(id integer primary key autoincrement,name text not null,path text not null);")
        db.execSQL("create table if not exists $TABLE_NAME2(id integer primary key autoincrement,level integer not null,sequence text not null,correctSeq text);")
        db.execSQL("create table if not exists $TABLE_NAME3(id integer primary key autoincrement,name text not null,correct_number integer,total_number integer);")
        db.execSQL("create table if not exists $TABLE_NAME4(id integer primary key autoincrement,level integer not null,sequence text not null,correct_number integer,total_number integer);")

        db.beginTransaction()
        try {
            var stmt: SQLiteStatement

            stmt = db.compileStatement("insert into shapers(name, path) values(?, ?);")
            for (shaper in SHAPERS) {
                stmt.bindString(1, shaper[0])
                stmt.bindString(2, shaper[1])
                stmt.executeInsert()
            }
            stmt = db.compileStatement("insert into sets(level, sequence, correctSeq) values(?, ?, ?);")
            for (set in SETS) {
                stmt.bindString(1, set[0])
                stmt.bindString(2, set[1])
                if (set[2] != null) {
                    stmt.bindString(3, set[2])
                } else {
                    stmt.bindNull(3)
                }
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        var c: Cursor = db.query(TABLE_NAME1, null, null, null, null, null, null)
        c.moveToFirst()
        while (!c.isAfterLast) {
            val contentValues = ContentValues()
            contentValues.put("name", c.getString(c.getColumnIndex("name")))
            contentValues.put("correct_number", 0)
            contentValues.put("total_number", -1)
            db.insert(TABLE_NAME3, null, contentValues)
            c.moveToNext()
        }
        c = db.query(TABLE_NAME2, null, null, null, null, null, null)
        c.moveToFirst()
        while (!c.isAfterLast) {
            val contentValues = ContentValues()
            contentValues.put("level", c.getString(c.getColumnIndex("level")))
            contentValues.put("sequence", c.getString(c.getColumnIndex("sequence")))
            contentValues.put("correct_number", 0)
            contentValues.put("total_number", -1)
            db.insert(TABLE_NAME4, null, contentValues)
            c.moveToNext()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVer: Int, newVer: Int) {
        val tag = "DBHelper.onUpgrade"
        db.execSQL("drop table if exists $TABLE_NAME1;")
        db.execSQL("drop table if exists $TABLE_NAME2;")
        onCreate(db)
        //TODO: TABLE_NAME3とTABLE_NAME4への変更箇所修正処理
        val cursorTable3 = db.rawQuery("select $TABLE_NAME1.id, $TABLE_NAME1.name, correct_number, total_number from $TABLE_NAME1 left outer join $TABLE_NAME3 on $TABLE_NAME1.name", null)
        cursorTable3.moveToFirst()
        val cursorTable4 = db.rawQuery("select $TABLE_NAME2.id, $TABLE_NAME2.level, $TABLE_NAME2.sequence, correct_number, total_number from $TABLE_NAME2 left outer join $TABLE_NAME4 on $TABLE_NAME2.sequence", null)
        cursorTable4.moveToFirst()
        db.execSQL("drop table if exists $TABLE_NAME3;")
        db.execSQL("drop table if exists $TABLE_NAME4;")
        onCreate(db)
        while (!cursorTable3.isAfterLast) {
            val name = cursorTable3.getInt(cursorTable3.getColumnIndex("name"))
            val correct_number: Int
            try {
                correct_number = cursorTable3.getInt(cursorTable3.getColumnIndex("correct_number"))
            } catch(e: Exception) {
                correct_number = 0
            }
            val total_number: Int
            try {
                total_number = cursorTable3.getInt(cursorTable3.getColumnIndex("total_number"))
            } catch(e: Exception) {
                total_number = -1
            }
            db.execSQL("insert into $TABLE_NAME3(name, correct_number, total_number) values($name, $correct_number, $total_number)")
            cursorTable3.moveToNext()
        }
        while (!cursorTable4.isAfterLast) {
            val level = cursorTable4.getInt(cursorTable4.getColumnIndex("level"))
            val sequence = cursorTable4.getInt(cursorTable4.getColumnIndex("sequence"))
            val correct_number: Int
            try {
                correct_number = cursorTable4.getInt(cursorTable4.getColumnIndex("correct_number"))
            } catch(e: Exception) {
                correct_number = 0
            }
            val total_number: Int
            try {
                total_number = cursorTable4.getInt(cursorTable4.getColumnIndex("total_number"))
            } catch(e: Exception) {
                total_number = -1
            }
            db.execSQL("insert into $TABLE_NAME4(level, sequence, correct_number, total_number) values($level, $sequence, $correct_number, $total_number)")
            cursorTable4.moveToNext()
        }
        Log.v(tag, "done")
    }

    companion object {
        val DB_NAME = "shaper.db"
        val TABLE_NAME1 = "shapers"
        val TABLE_NAME2 = "sets"
        val TABLE_NAME3 = "weakShapers"
        val TABLE_NAME4 = "weakSets"
        val DB_VERSION = 9
    }
}