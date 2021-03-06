package com.github.mvysny.karibudsl.v10

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.cloneBySerialization
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.expectRows
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.ironlist.IronList

class IronListTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    group("iron list dsl") {
        test("basic properties") {
            val il: IronList<String> = UI.getCurrent().ironList<String>()
            il.refresh()
            il.expectRows(0)
        }
    }

    test("serialization") {
        val il: IronList<Person> = IronList<Person>().apply {
            setRenderer { it.toString() }
        }
        il.cloneBySerialization()
    }
})
