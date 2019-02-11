package com.github.mvysny.karibudsl.v10

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._expectOne
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.caption
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.details.Details
import com.vaadin.flow.component.html.Span
import kotlin.streams.toList
import kotlin.test.expect

class DetailsTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("smoke") {
        UI.getCurrent().details("Hello") {
            content {
                button("hi!")
            }
        }
        _expectOne<Span>{ text = "Hello" }
        _expectOne<Button>{ caption = "hi!" }
        expect("hi!") { _get<Details>().content.toList().joinToString { it.caption } }
    }

    test("summary as component") {
        UI.getCurrent().details {
            summary {
                button("hi!")
            }
        }
        _expectOne<Button>{ caption = "hi!" }
        expect("hi!") { _get<Details>().summary.caption }
    }
})
