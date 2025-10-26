package name.abuchen.portfolio.ui.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.oauth.AuthenticationException;
import name.abuchen.portfolio.oauth.OAuthClient;
import name.abuchen.portfolio.oauth.OAuthURLInfo;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.DesktopAPI;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.OAuthInfoDialog;

public class AuthenticationRequiredDialog
{
    public static void open(Shell shell, Client client, List<Security> securities)
    {
        var dialog = new MessageDialog(shell, Messages.LabelInfo, null,
                        Messages.LabelAuthenticationRequiredToRetrieveHistoricalPrices, MessageDialog.WARNING, 0,
                        Messages.CmdLogin, Messages.CmdNotNow)
        {
            @Override
            protected Control createCustomArea(Composite parent)
            {
                Composite container = new Composite(parent, SWT.NONE);
                GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 100).applyTo(container);

                TableColumnLayout tableLayout = new TableColumnLayout();
                container.setLayout(tableLayout);

                var table = new Table(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
                table.setHeaderVisible(false);
                table.setLinesVisible(true);

                table.addListener(SWT.Selection, e -> table.deselectAll());
                table.addListener(SWT.MouseDown, e -> table.deselectAll());
                table.addListener(SWT.KeyDown, e -> table.deselectAll());

                var column = new org.eclipse.swt.widgets.TableColumn(table, SWT.NONE);
                tableLayout.setColumnData(column, new ColumnWeightData(100));

                for (var security : securities.stream().sorted(new Security.ByName()).toList())
                {
                    var item = new org.eclipse.swt.widgets.TableItem(table, SWT.NONE);
                    item.setText(0, security.getName());
                    item.setImage(LogoManager.instance().getDefaultColumnImage(security, client.getSettings()));
                }

                return container;
            }

        };

        var result = dialog.open();

        if (result == 0)
        {
            try
            {
                OAuthURLInfo urlInfo = OAuthClient.INSTANCE.prepareOAuthURLInfo();
                OAuthInfoDialog infoDialog = new OAuthInfoDialog(shell, urlInfo.getAuthorizationUrl(), urlInfo.getCallbackUrl());

                if (infoDialog.open() == org.eclipse.jface.window.Window.OK)
                {
                    OAuthClient.INSTANCE.signInWithInfo(DesktopAPI::browse, urlInfo);
                }
            }
            catch (AuthenticationException e)
            {
                PortfolioPlugin.log(e);
                MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.LabelError, e.getMessage());
            }
        }
    }

    private AuthenticationRequiredDialog()
    {
    }
}
