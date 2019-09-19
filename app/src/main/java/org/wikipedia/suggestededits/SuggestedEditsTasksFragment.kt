package org.wikipedia.suggestededits

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_tasks.*
import org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE
import org.wikipedia.Constants.InvokeSource.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.language.LanguageSettingsInvokeSource
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.FooterMarginItemDecoration
import java.util.*

class SuggestedEditsTasksFragment : Fragment() {
    private lateinit var addDescriptionsTask: SuggestedEditsTask
    private lateinit var translateDescriptionsTask: SuggestedEditsTask
    private lateinit var addImageCaptionsTask: SuggestedEditsTask
    private lateinit var translateImageCaptionsTask: SuggestedEditsTask
    private lateinit var multilingualTeaserTask: SuggestedEditsTask

    private val displayedTasks = ArrayList<SuggestedEditsTask>()
    private val callback = TaskViewCallback()

    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.elevation = 0f

        swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        swipeRefreshLayout.setOnRefreshListener{ this.updateUI() }

        tasksRecyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val topDecorationDp = 16
        tasksRecyclerView.addItemDecoration(FooterMarginItemDecoration(0, topDecorationDp))
        tasksRecyclerView.adapter = RecyclerAdapter(displayedTasks)

        encouragementMessage.text=getString(R.string.suggested_edits_encouragement_message,AccountUtil.getUserName())

        //usernameText.text = AccountUtil.getUserName()
       /* userContributionsButton.setOnClickListener {
            startActivity(SuggestedEditsContributionsActivity.newIntent(requireContext()))
        }*/
        setUpTasks()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_suggested_edits_tasks, menu)
        var drawable: Drawable = menu.findItem(R.id.menu_help).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, ResourceUtil.getThemedColor(context!!, R.attr.colorAccent));
        menu.findItem(R.id.menu_help).setIcon(drawable);
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun fetchUserContributions() {
        //contributionsText.visibility = View.GONE
        //progressBar.visibility = View.VISIBLE

        disposables.add(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).editorTaskCounts
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    //progressBar.visibility = View.GONE
                    //contributionsText.visibility = View.VISIBLE
                    swipeRefreshLayout.isRefreshing = false
                }
                .subscribe({ response ->
                    val editorTaskCounts = response.query()!!.editorTaskCounts()!!
                    var totalEdits = 0
                    for (count in editorTaskCounts.descriptionEditsPerLanguage.values) {
                        totalEdits += count
                    }
                    for (count in editorTaskCounts.captionEditsPerLanguage.values) {
                        totalEdits += count
                    }
                    //contributionsText.text = resources.getQuantityString(R.plurals.suggested_edits_contribution_count, totalEdits, totalEdits)
                }, { throwable ->
                    L.e(throwable)
                    FeedbackUtil.showError(requireActivity(), throwable)
                }))
    }

    private fun updateUI() {
        requireActivity().invalidateOptionsMenu()
        fetchUserContributions()
    }

    private fun setUpTasks() {
        addDescriptionsTask = SuggestedEditsTask()
        addDescriptionsTask.title = getString(R.string.description_edit_tutorial_title_descriptions)
        addDescriptionsTask.description = getString(R.string.suggested_edits_add_descriptions_task_detail)
        addDescriptionsTask.imageDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_short_text_white_24dp)

        addImageCaptionsTask = SuggestedEditsTask()
        addImageCaptionsTask.title = getString(R.string.suggested_edits_image_captions)
        addImageCaptionsTask.description = getString(R.string.suggested_edits_image_captions_task_detail)
        addImageCaptionsTask.imageDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_icon_caption_images)
    }


    private inner class TaskViewCallback : SuggestedEditsTaskView.Callback {
        override fun onPositiveActionClick(task: SuggestedEditsTask) {
            if (task == multilingualTeaserTask) {
                requireActivity().startActivityForResult(WikipediaLanguagesActivity.newIntent(requireActivity(),
                        LanguageSettingsInvokeSource.DESCRIPTION_EDITING.text()), ACTIVITY_REQUEST_ADD_A_LANGUAGE)
            }
        }

        override fun onNegativeActionClick(task: SuggestedEditsTask) {
            if (task == multilingualTeaserTask) {
                val multilingualTaskPosition = displayedTasks.indexOf(multilingualTeaserTask)
                displayedTasks.remove(multilingualTeaserTask)
                tasksRecyclerView.adapter!!.notifyItemChanged(multilingualTaskPosition)
                Prefs.setShowSuggestedEditsMultilingualTeaserTask(false)
            }
        }

        override fun onViewClick(task: SuggestedEditsTask) {
            if (task == addDescriptionsTask) {
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), SUGGESTED_EDITS_ADD_DESC))
            } else if (task == addImageCaptionsTask) {
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), SUGGESTED_EDITS_ADD_CAPTION))
            } else if (task == translateDescriptionsTask && WikipediaApp.getInstance().language().appLanguageCodes.size > 1) {
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), SUGGESTED_EDITS_TRANSLATE_DESC))
            } else if (task == translateImageCaptionsTask && WikipediaApp.getInstance().language().appLanguageCodes.size > 1) {
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), SUGGESTED_EDITS_TRANSLATE_CAPTION))
            }
        }
    }

    internal inner class RecyclerAdapter(tasks: List<SuggestedEditsTask>) : DefaultRecyclerAdapter<SuggestedEditsTask, SuggestedEditsTaskView>(tasks) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<SuggestedEditsTaskView> {
            return DefaultViewHolder(SuggestedEditsTaskView(parent.context))
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<SuggestedEditsTaskView>, i: Int) {
            holder.view.setUpViews(items()[i], callback)
        }
    }

    companion object {
        fun newInstance(): SuggestedEditsTasksFragment {
            return SuggestedEditsTasksFragment()
        }
    }
}
