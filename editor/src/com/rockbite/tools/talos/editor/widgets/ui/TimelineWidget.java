/*******************************************************************************
 * Copyright 2019 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.rockbite.tools.talos.editor.widgets.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.rockbite.tools.talos.TalosMain;
import com.rockbite.tools.talos.editor.ParticleEmitterWrapper;
import com.rockbite.tools.talos.runtime.ParticleEmitterInstance;

public class TimelineWidget extends Table {

    Table listContent;

    Array<EmitterRow> rows = new Array<>();

    EmitterRow selectedRow;

    public TimelineWidget(Skin skin) {
        setSkin(skin);

        Table topBar = new Table();
        topBar.setBackground(getSkin().getDrawable("seekbar-background"));
        TextButton buttonAdd = new TextButton("+", skin);
        TextButton buttonDel = new TextButton("-", skin);
        TextButton buttonUp = new TextButton("up", skin);
        TextButton buttonDown = new TextButton("down", skin);

        topBar.add(buttonAdd).left().width(30).pad(3);
        topBar.add(buttonDel).left().width(30).pad(3);
        topBar.add(buttonUp).left().width(30).pad(3);
        topBar.add(buttonDown).left().width(45).pad(3);

        topBar.add().growX();
        add(topBar).growX();
        row();

        listContent = new Table();
        ScrollPane scrollPane = new ScrollPane(listContent, getSkin());
        add(scrollPane).grow();

        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                if(!event.isHandled()) {
                    for(EmitterRow row: rows) {
                        row.setStaticMode();
                    }
                }
            }
        });


        setTouchable(Touchable.enabled);

        buttonAdd.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                final ParticleEmitterWrapper emitter = TalosMain.Instance().TalosProject().createNewEmitter("emitter", -1);
                selectRow(emitter);
            }
        });

        buttonDel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                ParticleEmitterWrapper wrapper = getSelectedRow().wrapper;

                TalosMain.Instance().TalosProject().removeEmitter(wrapper);
            }
        });

        buttonUp.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                ParticleEmitterWrapper wrapper = getSelectedRow().wrapper;
                moveWrapperSortingPosition(wrapper, -1);
            }
        });

        buttonDown.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                ParticleEmitterWrapper wrapper = getSelectedRow().wrapper;
                moveWrapperSortingPosition(wrapper, 1);

            }
        });
    }

    public void moveWrapperSortingPosition(ParticleEmitterWrapper wrapper, int changeBy) { // -1 or 1
        if(changeBy < -1) changeBy = -1;
        if(changeBy > 1) changeBy = 1;

        int pos = wrapper.getEmitter().getSortPosition();
        int newPos = pos + changeBy;

        Array<ParticleEmitterInstance> emitters = TalosMain.Instance().TalosProject().getParticleEffect().getEmitters();

        if(newPos < 0 || newPos > emitters.size-1) return;

        // let's swap
        ParticleEmitterInstance emOne = emitters.get(pos);
        ParticleEmitterInstance emTwo = emitters.get(newPos);
        int tmp = emOne.emitterGraph.getSortPosition();
        emOne.emitterGraph.setSortPosition(emTwo.emitterGraph.getSortPosition());
        emTwo.emitterGraph.setSortPosition(tmp);

        TalosMain.Instance().TalosProject().sortEmitters();
    }

    public void setEmitters(Array<ParticleEmitterWrapper> emitterWrappers) {
        listContent.clearChildren();

        for(ParticleEmitterWrapper emitter: emitterWrappers) {
            EmitterRow row = new EmitterRow(this, getSkin());
            row.set(emitter);
            listContent.add(row).growX();

            rows.add(row);
            listContent.row().padTop(0);
        }


        listContent.row();
        listContent.add().expandY();

        if(emitterWrappers.size > 0) {
            selectRow(TalosMain.Instance().TalosProject().getCurrentEmitterWrapper());
        }
    }

    public void selectRow(ParticleEmitterWrapper emitterWrapper) {
        for(EmitterRow row: rows) {
            if(row.wrapper == emitterWrapper) {
                row.select();
                selectedRow = row;
            } else {
                row.unselect();
            }
        }

        TalosMain.Instance().TalosProject().setCurrentEmitterWrapper(emitterWrapper);
        TalosMain.Instance().NodeStage().moduleBoardWidget.setCurrentEmitter(emitterWrapper);
    }

    public EmitterRow getSelectedRow() {
        return selectedRow;
    }

    public void clearEmitters() {
        rows.clear();
        selectedRow = null;
    }

    private class EmitterRow extends Table {

        private Label label;
        private TextField textField;

        private Table labelTabel;
        private Table inputTable;

        ImageButton eye;

        private boolean selecteed = true;

        private ParticleEmitterWrapper wrapper;
        private TimelineWidget timeline;

        public EmitterRow(final TimelineWidget timeline, Skin skin) {
            this.timeline = timeline;
            setSkin(skin);
            setBackground(getSkin().getDrawable("red_row"));

            eye = new ImageButton(getSkin(), "eye");
            eye.setChecked(true);

            Stack stack = new Stack();

            labelTabel = new Table();
            inputTable = new Table();

            stack.add(labelTabel);
            stack.add(inputTable);

            add(eye).padLeft(5);
            add(stack).grow();

            label = new Label("", getSkin());
            labelTabel.add(label).pad(2).padLeft(4).left();
            labelTabel.add().expandX();


            textField = new TextField("", getSkin(), "no-bg");
            inputTable.add(textField).pad(2).padLeft(4).left();
            inputTable.add().expandX();

            setStaticMode();

            addListener(new ClickListener() {

                long clickTime = 0;

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    long time = TimeUtils.millis();

                    timeline.selectRow(wrapper);

                    if(time - clickTime < 200) {
                        setEditMode();
                    }

                    clickTime = time;


                    super.clicked(event, x, y);
                }
            });

            eye.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    TalosMain.Instance().TalosProject().getParticleEffect().getEmitter(wrapper.getEmitter()).setVisible(!isMuted());
                }
            });

            textField.addListener(new InputListener() {
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if(keycode == Input.Keys.ENTER) {
                        EmitterRow.this.setStaticMode();
                    }

                    return super.keyDown(event, keycode);
                }
            });

            textField.addListener(new FocusListener() {
                @Override
                public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                    super.keyboardFocusChanged(event, actor, focused);
                    if(!focused) {
                        setStaticMode();
                    }
                }
            });

            select();
            setTouchable(Touchable.enabled);
        }

        public void select() {
            selecteed = true;
            setBackground(getSkin().getDrawable("orange_row"));
        }

        public void unselect() {
            selecteed = false;
            setBackground(getSkin().getDrawable("red_row"));
        }

        public void setEditMode() {
            labelTabel.setVisible(false);
            inputTable.setVisible(true);
            textField.setText(label.getText().toString());
            textField.selectAll();
            getStage().setKeyboardFocus(textField);
            TalosMain.Instance().NodeStage().getStage().unfocusAll();
        }

        public void setStaticMode() {
            labelTabel.setVisible(true);
            inputTable.setVisible(false);

            label.setText(textField.getText());
            textField.clearSelection();

            if(wrapper != null) {
                wrapper.setName(textField.getText());
            }
        }

        public void setMuted(boolean muted) {
            eye.setChecked(!muted);
        }

        public boolean isMuted() {
            return !eye.isChecked();
        }

        public void set(ParticleEmitterWrapper emitter) {
            label.setText(emitter.getName());
            textField.setText(emitter.getName());
            wrapper = emitter;
        }
    }
}
