package com.karimun.fordperformanceact.Fragments;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karimun.fordperformanceact.Adapters.EventAdapter;
import com.karimun.fordperformanceact.Adapters.TimePickerAdapter;
import com.karimun.fordperformanceact.MainActivity;
import com.karimun.fordperformanceact.Models.Member;
import com.karimun.fordperformanceact.Models.OurEvent;
import com.karimun.fordperformanceact.R;
import com.karimun.fordperformanceact.Functionalities.SwipeToDeleteCallback;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class EventCalendarFragment extends Fragment {

    AlertDialog createEventWindow;
    public static EditText timeStartField;
    EditText dateStartField;
    public static EditText timeEndField;
    EditText dateEndField;
    public static boolean isTimeStartField = false;

    TextView labelMonthAndYear;
    ImageView calendarPrevious, calendarNext;

    Calendar calendarStart;
    Calendar calendarEnd;

    int yearEnd;
    int monthEnd;
    int dayOfMonthEnd;

    String dayOfWeekStart;

    List<OurEvent> events;
    List<Event> highlightedDateEvents;

    FirebaseUser fUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_event_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        MainActivity.toggle.setDrawerIndicatorEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_hamburger);
        MainActivity.appBarLayout.setVisibility(View.VISIBLE);

        fUser = FirebaseAuth.getInstance().getCurrentUser();

        highlightedDateEvents = new ArrayList<>();
        events = new ArrayList<>();

        final TextView noEventText = view.findViewById(R.id.no_event_text);

        final RecyclerView recyclerView = view.findViewById(R.id.recycle_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        // Declare and set calendar view including month and year labels
        labelMonthAndYear = view.findViewById(R.id.label_month_and_year);
        calendarPrevious = view.findViewById(R.id.calendar_previous);
        calendarNext = view.findViewById(R.id.calendar_next);
        final CompactCalendarView calendarView = view.findViewById(R.id.compactcalendar_view);
        calendarView.setUseThreeLetterAbbreviation(true);
        setEventDateOnCalendar(calendarView);

        // Set current month and year
        SimpleDateFormat monthAndYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        labelMonthAndYear.setText(monthAndYearFormat.format(calendarView.getFirstDayOfCurrentMonth()));

        // Give previous button a functionality to go back to previous month or year
        calendarPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendarView.scrollLeft();
            }
        });

        // Give next button a functionality to go to next month or year
        calendarNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendarView.scrollRight();
            }
        });

        listAllEvents(recyclerView, noEventText);

        // When this button is clicked, there will be a pop up dialog asking for event details
        Button btnCreateNewEvent = view.findViewById(R.id.create_new_event);
        btnCreateNewEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (getContext() != null) {

                    // Create dialog builder, that is to say, we are creating the content of the pop up dialog/window.
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setView(R.layout.window_create_event);


                    createEventWindow = builder.create();
                    createEventWindow.show();

                    final CheckBox allDayCheckBox = createEventWindow.findViewById(R.id.all_day_checkbox);
                    final CheckBox sendNotificationCheckBox = createEventWindow.findViewById(R.id.send_notification_checkbox);
                    final LinearLayout timeEndWrapper = createEventWindow.findViewById(R.id.time_end_wrapper);
                    final EditText eventTitleField = createEventWindow.findViewById(R.id.title_field);
                    dateStartField = createEventWindow.findViewById(R.id.date_start_field);
                    timeStartField = createEventWindow.findViewById(R.id.time_start_field);
                    dateEndField = createEventWindow.findViewById(R.id.date_end_field);
                    timeEndField = createEventWindow.findViewById(R.id.time_end_field);
                    final EditText eventLocationField = createEventWindow.findViewById(R.id.location_field);
                    Button btnCreateEvent = createEventWindow.findViewById(R.id.btn_create_event);


                    if (allDayCheckBox != null) {

                        allDayCheckBox.setTextColor(getResources().getColor(R.color.colorGeneralText));

                        // When the allDayCheckBox is ticked, we make other fields invisible and remove their inputs/contents.
                        allDayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                                if (allDayCheckBox.isChecked() && timeEndWrapper != null && timeStartField != null && dateEndField != null) {

                                    timeStartField.setVisibility(View.GONE);
                                    timeEndWrapper.setVisibility(View.GONE);
                                    timeStartField.setText("");
                                    dateEndField.setText("");
                                    timeEndField.setText("");
                                } else if (!allDayCheckBox.isChecked() && timeEndWrapper != null && timeStartField != null) {

                                    timeStartField.setVisibility(View.VISIBLE);
                                    timeEndWrapper.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    }

                    if (dateStartField != null) {

                        // When this date start field is clicked, we open a pop up(sub)dialog for user to pick date
                        dateStartField.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                pickStartDate(dateStartField);
                            }
                        });
                    }

                    if (timeStartField != null) {

                        // When this time start field is clicked, we open a pop up (sub)dialog for user to pick time
                        timeStartField.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                isTimeStartField = true;

                                if (getFragmentManager() != null) {
                                    TimePickerAdapter timeStartPicker = new TimePickerAdapter();
                                    timeStartPicker.show(getFragmentManager(), "timePicker");
                                }
                            }
                        });
                    }

                    if (dateEndField != null) {

                        dateEndField.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {

                                pickEndDate(dateEndField);
                            }
                        });
                    }

                    if (timeEndField != null) {

                        timeEndField.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                isTimeStartField = false;

                                if (getFragmentManager() != null) {
                                    TimePickerAdapter timeEndPicker = new TimePickerAdapter();
                                    timeEndPicker.show(getFragmentManager(), "timePicker");
                                }
                            }
                        });
                    }

                    // When the create event button within the window dialog is pressed, then we push new data (e.g. title, startDate etc) to database
                    if (btnCreateEvent != null) {
                        btnCreateEvent.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Event").push();

                                HashMap<String, Object> hashMap = new HashMap<>();

                                if (eventTitleField != null
                                        && dateStartField != null && timeStartField != null
                                        && dateEndField != null && timeEndField != null
                                        && eventLocationField != null
                                        && sendNotificationCheckBox != null) {

                                    if (!TextUtils.isEmpty(eventTitleField.getText()) && !TextUtils.isEmpty(dateStartField.getText())
                                            && !TextUtils.isEmpty(eventLocationField.getText())) {

                                        if (allDayCheckBox != null && allDayCheckBox.isChecked()) {

                                            hashMap.put("eventId", reference.getKey());
                                            hashMap.put("title", eventTitleField.getText().toString());
                                            hashMap.put("dateStart", changeDateFormat(dateStartField.getText().toString()));
                                            hashMap.put("timeStart", timeStartField.getText().toString());
                                            hashMap.put("dateEnd", changeDateFormat(dateEndField.getText().toString()));
                                            hashMap.put("timeEnd", timeEndField.getText().toString());
                                            hashMap.put("location", eventLocationField.getText().toString());
                                            hashMap.put("dayOfWeekStart", dayOfWeekStart);
                                            hashMap.put("isEventArchived", false);

                                            if (sendNotificationCheckBox.isChecked()) {
                                                hashMap.put("sendNotification", true);
                                            } else {
                                                hashMap.put("sendNotification", false);
                                            }

                                            reference.updateChildren(hashMap);

                                            Toast.makeText(getContext(), "Event has been successfully created.", Toast.LENGTH_SHORT).show();
                                            createEventWindow.dismiss();
                                        } else if (dateStartField.getText().toString().equals(dateEndField.getText().toString())) {

                                            if (convertStringToDateForTime(timeStartField.getText().toString())
                                                    .compareTo(convertStringToDateForTime(timeEndField.getText().toString())) <= 0) {

                                                hashMap.put("eventId", reference.getKey());
                                                hashMap.put("title", eventTitleField.getText().toString());
                                                hashMap.put("dateStart", changeDateFormat(dateStartField.getText().toString()));
                                                hashMap.put("timeStart", timeStartField.getText().toString());
                                                hashMap.put("dateEnd", changeDateFormat(dateEndField.getText().toString()));
                                                hashMap.put("timeEnd", timeEndField.getText().toString());
                                                hashMap.put("location", eventLocationField.getText().toString());
                                                hashMap.put("dayOfWeekStart", dayOfWeekStart);
                                                hashMap.put("isEventArchived", false);

                                                if (sendNotificationCheckBox.isChecked()) {
                                                    hashMap.put("sendNotification", true);
                                                } else {
                                                    hashMap.put("sendNotification", false);
                                                }

                                                reference.updateChildren(hashMap);

                                                Toast.makeText(getContext(), "Event has been successfully created.", Toast.LENGTH_SHORT).show();
                                                createEventWindow.dismiss();
                                            } else {
                                                Toast.makeText(getContext(), "Must pick time after start time", Toast.LENGTH_SHORT).show();
                                            }
                                        } else if (convertStringToDateForDate(dateStartField.getText().toString())
                                                .compareTo(convertStringToDateForDate(dateEndField.getText().toString())) < 0) {

                                            hashMap.put("eventId", reference.getKey());
                                            hashMap.put("title", eventTitleField.getText().toString());
                                            hashMap.put("dateStart", changeDateFormat(dateStartField.getText().toString()));
                                            hashMap.put("timeStart", timeStartField.getText().toString());
                                            hashMap.put("dateEnd", changeDateFormat(dateEndField.getText().toString()));
                                            hashMap.put("timeEnd", timeEndField.getText().toString());
                                            hashMap.put("location", eventLocationField.getText().toString());
                                            hashMap.put("dayOfWeekStart", dayOfWeekStart);
                                            hashMap.put("isEventArchived", false);

                                            if (sendNotificationCheckBox.isChecked()) {
                                                hashMap.put("sendNotification", true);
                                            } else {
                                                hashMap.put("sendNotification", false);
                                            }

                                            reference.updateChildren(hashMap);

                                            Toast.makeText(getContext(), "Event has been successfully created.", Toast.LENGTH_SHORT).show();
                                            createEventWindow.dismiss();
                                        } else {
                                            Toast.makeText(getContext(), "Start date must not occur after end date.", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {

                                        Toast.makeText(getContext(), "Title, Start date, and Location must not be empty.", Toast.LENGTH_LONG).show();
                                    }
                                }

                            }
                        });
                    }
                }
            }
        });


    }

    // Method for picking start date
    private void pickStartDate(final EditText dateInput) {

        calendarStart = Calendar.getInstance();

        final DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

                calendarStart.set(Calendar.YEAR, year);
                calendarStart.set(Calendar.MONTH, month);
                calendarStart.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                dateInput.setText(dateFormat.format(calendarStart.getTime()));

                SimpleDateFormat dateFormat2 = new SimpleDateFormat("EEE", Locale.getDefault());
                Date date = new Date(year, month, dayOfMonth - 1);
                dayOfWeekStart = dateFormat2.format(date);
            }
        };


        if (getContext() != null) {

            int year = calendarStart.get(Calendar.YEAR);
            int month = calendarStart.get(Calendar.MONTH);
            int dayOfMonth = calendarStart.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dateStartDialog
                    = new DatePickerDialog(getContext(), R.style.MyDatePickerDialog, onDateSetListener, year, month, dayOfMonth);

            dateStartDialog.getDatePicker().setMinDate(System.currentTimeMillis());

            dateStartDialog.show();
        }
    }

    // Method for picking end date
    private void pickEndDate(final EditText dateInput) {

        calendarEnd = Calendar.getInstance();

        final DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

                calendarEnd.set(Calendar.YEAR, year);
                calendarEnd.set(Calendar.MONTH, month);
                calendarEnd.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                dateInput.setText(dateFormat.format(calendarEnd.getTime()));
            }
        };

        if (getContext() != null) {

            yearEnd = calendarEnd.get(Calendar.YEAR);
            monthEnd = calendarEnd.get(Calendar.MONTH);
            dayOfMonthEnd = calendarEnd.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dateEndDialog
                    = new DatePickerDialog(getContext(), R.style.MyDatePickerDialog, onDateSetListener, yearEnd, monthEnd, dayOfMonthEnd);

            if (dateStartField.getText().length() > 0 && dateStartField.getText() != null) {
                dateEndDialog.getDatePicker().setMinDate(calendarStart.getTimeInMillis());
            } else {
                dateEndDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            }

            dateEndDialog.show();
        }
    }

    // Method for changing date format
    private String changeDateFormat(String formerDateFormat) {
        try {
            SimpleDateFormat former = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date parsedFormerDate = former.parse(formerDateFormat);
            SimpleDateFormat current = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            return current.format(parsedFormerDate);
        } catch (ParseException e) {
            Log.d("PARSE EXCEPTION", e.getMessage());
        }
        return "";
    }

    // Method for converting string object to date for date fields
    private Date convertStringToDateForDate(String str) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return dateFormat.parse(str);
        } catch (ParseException e) {
            Log.d("PARSE EXCEPTION", e.getMessage());
        }
        return null;
    }

    // Method for converting string object to date for time fields
    private Date convertStringToDateForTime(String str) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return timeFormat.parse(str);
        } catch (ParseException e) {
            Log.d("PARSE EXCEPTION", e.getMessage());
        }
        return null;
    }

    // Highlight event dates on calendar
    private void setEventDateOnCalendar(final CompactCalendarView ccv) {


        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Event");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    final OurEvent ourEvent = snapshot.getValue(OurEvent.class);

                    try {
                        if (ourEvent != null) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                            Date eventDate = dateFormat.parse(ourEvent.getDateStart());
                            Event event = new Event(Color.parseColor("#00D1FF"), eventDate.getTime(), ourEvent.getTitle());
                            highlightedDateEvents.add(event);
                        }
                    } catch (ParseException e) {
                        Log.d("PARSE EXCEPTION", e.getMessage());
                    }
                }
                ccv.addEvents(highlightedDateEvents);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        ccv.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {

            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {

                SimpleDateFormat monthAndYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                labelMonthAndYear.setText(monthAndYearFormat.format(firstDayOfNewMonth));
            }
        });
    }

    // List all events
    public void listAllEvents(final RecyclerView recyclerView, final TextView noEventText) {
        // Fetch event data from database and add it to event list
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Event");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                events.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    OurEvent ourEvent = snapshot.getValue(OurEvent.class);

                    if (ourEvent != null && !ourEvent.isEventArchived()) {
                        events.add(ourEvent);


                        // Declare the event adapter object
                        final EventAdapter eventAdapter = new EventAdapter(getContext(), events, false);

                        // Set event adapter to recyclerview
                        recyclerView.setAdapter(eventAdapter);

                        // Set event lists to be deletable if member is admin
                        DatabaseReference memberReference = FirebaseDatabase.getInstance().getReference("Member").child(fUser.getUid());
                        memberReference.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Member member = dataSnapshot.getValue(Member.class);

                                if (member != null && member.isAdmin()) {
                                    SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(eventAdapter);
                                    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeToDeleteCallback);
                                    itemTouchHelper.attachToRecyclerView(recyclerView);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
                // If there is one or more events, recyclerView is visible. If not, there will be text indicating there is no event
                if (events.size() > 0) {
                    noEventText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    noEventText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        if (createEventWindow != null)
            createEventWindow.dismiss();
    }
}
